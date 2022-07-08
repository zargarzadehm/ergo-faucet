package controllers

import akka.actor.ActorSystem
import javax.inject._
import play.api.Logger
import play.api.mvc._
import io.circe.Json
import play.api.libs.circe.Circe
import play.filters.csrf.CSRF
import scala.collection.mutable
import scala.concurrent.ExecutionContext

import models.{DiscordTokenObj, TokenPayment}
import utils.Util._
import utils.{Conf, CreateReward, Discord}
import controllers.actions.{TokenAction, UserAction, UserActionOption}
import dao._

@Singleton
class Controller @Inject()(userAction: UserAction, userActionOption: UserActionOption, tokenAction: TokenAction, assets: Assets, sessionDao: SessionDAO, userDAO: UserDAO, paymentTokenDao: PaymentTokenDAO, cc: ControllerComponents, actorSystem: ActorSystem, createReward: CreateReward)(implicit exec: ExecutionContext) extends AbstractController(cc) with Circe {

    private val logger: Logger = Logger(this.getClass)

    def okException(e: Throwable): Result = {
        logger.info(s"error in controller ${e.getMessage}")
        Ok(s"""{"success": false, "message": "${e.getMessage}"}""").as("application/json")
    }

    def badException(e: Throwable): Result = {
        logger.error(s"error in controller ${getStackTraceStr(e)}")
        Ok(s"""{"success": false, "message": "${e.getMessage}"}""").as("application/json")
    }

    def medException(e: Throwable): Result = {
        logger.info(s"error in controller ${e.getMessage}")
        BadRequest(s"""{"success": false, "message": "${e.getMessage}"}""").as("application/json")
    }

    def index: Action[AnyContent] = assets.at("index.html")

    def oathRedirect: Action[AnyContent] = assets.at("oauth.html")

    def assetOrDefault(resource: String): Action[AnyContent] = {
        if (resource.contains(".")) assets.at(resource) else index
    }

    /**
     * get info
     */
    def info: Action[AnyContent] = tokenAction.andThen(userActionOption) { request =>
        try {
            var buttonString = "{\"buttons\":["
            Conf.buttons.foreach(button => {
                val res =
                    s"""{\"name\": \"${button.name}\",
                       |\"active\": ${button.active},
                       |\"url\": \"${button.url}\"},""".stripMargin
                buttonString += res
            })
            buttonString = buttonString.substring(0, buttonString.length - 1)
            buttonString += "],"
            buttonString += s"""\"mainButton\": \"${Conf.mainButton}\","""
            buttonString += s"""\"title\": \"${Conf.title}\","""
            buttonString += s"""\"siteKey\": \"${Conf.siteKey}\","""
            buttonString += s"""\"discordRequired\": ${Conf.discordConf.active}"""
            request.user match {
                case Some(user) =>
                    val userData =
                        s""", {\"email\": \"${user.email}\",
                           |\"username\": \"${user.username}\",
                           |\"id\": \"${user.discordId}\",
                           |\"verified\": ${user.verified}}""".stripMargin
                    buttonString += s"""\"user\": ${userData}"""
                case None =>
                    if (Conf.discordConf.active)
                        buttonString += s""", \"oauthUrl\": \"${Conf.discordConf.oauthLink}\""""
            }
            buttonString += "}"
            Ok(s"""$buttonString""".stripMargin).as("application/json")
        }
        catch {
            case e: Throwable => badException(e)
        }
    }

    /**
     * get list of supported assets
     */
    def supportedAssets: Action[AnyContent] = Action { implicit request =>
        try {
            var assetString = "{"
            Conf.ergoAssets.foreach(asset => {
                val res = s"""\"${asset._1}\": \"${asset._2.name}\","""
                assetString += res
            })
            assetString = assetString.substring(0, assetString.length - 1)
            assetString += "}"
            Ok(s"""$assetString""".stripMargin).as("application/json")
        }
        catch {
            case e: Throwable => badException(e)
        }
    }

    /**
     * Send all assets
     */
    def assetPayment: Action[Json] = tokenAction(circe.json).andThen(userAction) { implicit request =>
        try {
            val challenge = request.body.hcursor.downField("challenge").as[String].getOrElse(throw new Throwable("Challenge field must exist"))
            val address = request.body.hcursor.downField("address").as[String].getOrElse(throw new Throwable("address field must exist"))
            val assetId = request.body.hcursor.downField("assetId").as[String].getOrElse(throw new Throwable("assetId field must exist"))
            var responseTxId: String = ""
            verifyRecaptcha(challenge)
            if (Conf.discordConf.active) {
                val user = request.user.get
                if (paymentTokenDao.exists(user.username, address, request.ip, Conf.ergoAssets(assetId.toInt).name)) {
                    BadRequest(
                        s"""{
                           |  "message": "This address has already received ${Conf.ergoAssets(assetId.toInt).name} assets."
                           |}""".stripMargin
                    ).as("application/json")
                }
                else {
                    val proxy_info = selectRandomProxyInfo(Conf.proxyInfos)
                    responseTxId = createReward.sendAsset(address, proxy_info.get, Conf.ergoAssets(assetId.toInt)).replaceAll("\"", "")
                    if (responseTxId.nonEmpty) {
                        paymentTokenDao.insertConsiderOldPayment(TokenPayment(
                            user.username,
                            address,
                            Conf.ergoAssets(assetId.toInt).assets("erg"),
                            Conf.ergoAssets(assetId.toInt).name,
                            Some(request.ip),
                            responseTxId
                        ))
                    }
                    else throw WaitException()
                }
            }
            else {
                val proxy_info = selectRandomProxyInfo(Conf.proxyInfos)
                responseTxId = createReward.sendAsset(address, proxy_info.get, Conf.ergoAssets(assetId.toInt)).replaceAll("\"", "")
                if (responseTxId.nonEmpty) {
                    paymentTokenDao.insertConsiderOldPayment(TokenPayment(
                        "testnet-fake",
                        address,
                        Conf.ergoAssets(assetId.toInt).assets("erg"),
                        Conf.ergoAssets(assetId.toInt).name,
                        Some(request.ip),
                        responseTxId
                    ))
                }
                else throw WaitException()
            }
            Ok(
                s"""{
                   |  "txId": "${Conf.explorerFrontUrl}/en/transactions/${responseTxId}"
                   |}""".stripMargin
            ).as("application/json")

        } catch {
            case e: WaitException => okException(e)
            case e: InvalidAddressException => medException(e)
            case e: InvalidRecaptchaException => medException(e)
            case e: DuplicateRequestException => medException(e)
            case e: Throwable => badException(e)
        }
    }

    /**
     * Discord oauth2 authentication
     */
    def auth(code: String): Action[AnyContent] = Action { implicit request =>
        try {
            if (code.nonEmpty) {
                var discordToken = Discord.getTokenOauth(code = code).getOrElse(throw AuthException())
                val userInfo = Discord.getUserData(discordToken).getOrElse(throw AuthException())
                discordToken = discordToken.copy(userInfo.username)
                sessionDao.insertUserSession(discordToken, userInfo)
                val csrfToken = CSRF.getToken.get.value
                val newSession = (DiscordTokenObj.unapply(discordToken) ++ mutable.Map("csrfToken" -> csrfToken)).toSeq
                Redirect("/oauth").withSession(newSession: _*)
            }
            else {
                Redirect(Conf.discordConf.oauthLink)
            }
        }
        catch {
            case e: Throwable => badException(e)
        }
    }

    /**
     * clear session (logout)
     */
    def logout: Action[AnyContent] = Action { implicit request =>
        try {
            Ok(
                s"""{
                   |  "status": "ok"
                   |}""".stripMargin).as("application/json").withNewSession
        }
        catch {
            case e: Throwable => badException(e)
        }
    }

}
