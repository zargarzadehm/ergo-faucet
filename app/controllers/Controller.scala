package controllers

import akka.actor.ActorSystem
import dao._
import javax.inject._
import models.{Payment, TokenPayment}
import play.api.Logger
import play.api.mvc._
import utils.Util._
import utils.{Conf, CreateReward}
import io.circe.Json
import io.circe.syntax._
import play.api.libs.circe.Circe

import scala.concurrent.ExecutionContext

@Singleton
class Controller @Inject()(assets: Assets, paymentErgDao: PaymentErgDAO, paymentTokenDao: PaymentTokenDAO, cc: ControllerComponents, actorSystem: ActorSystem, createReward: CreateReward)(implicit exec: ExecutionContext) extends AbstractController(cc) with Circe {

  private val logger: Logger = Logger(this.getClass)

  def okException(e: Throwable): Result = {
    logger.info(s"error in controller ${e.getMessage}")
    Ok(s"""{"success": false, "message": "${e.getMessage}"}""").as("application/json")
  }
  def badException(e: Throwable): Result = {
    logger.error(s"error in controller ${getStackTraceStr(e)}")
    BadRequest(s"""{"success": false, "message": "${e.getMessage}"}""").as("application/json")
  }
  def medException(e: Throwable): Result = {
    logger.info(s"error in controller ${e.getMessage}")
    BadRequest(s"""{"success": false, "message": "${e.getMessage}"}""").as("application/json")
  }

  def index: Action[AnyContent] = assets.at("index.html")

  def assetOrDefault(resource: String): Action[AnyContent] = {
    if (resource.contains(".")) assets.at(resource) else index
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
      assetString = assetString.substring(0, assetString.length-1)
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
  def assetPayment: Action[Json] = Action(circe.json) { implicit request =>
    try {
      val challenge = request.body.hcursor.downField("challenge").as[String].getOrElse(throw new Throwable("Challenge field must exist"))
      val address = request.body.hcursor.downField("address").as[String].getOrElse(throw new Throwable("address field must exist"))
      val assetId = request.body.hcursor.downField("assetId").as[String].getOrElse(throw new Throwable("assetId field must exist"))
      verifyRecaptcha(challenge)
      if (paymentTokenDao.exists(address, Conf.ergoAssets(assetId.toInt).name)) {
        BadRequest(
          s"""{
             |  "message": "This address has already received ${Conf.ergoAssets(assetId.toInt).name} assets."
             |}""".stripMargin
        ).as("application/json")
      }
      else {
        val proxy_info = selectRandomProxyInfo(Conf.proxyInfos)
        val txId = createReward.sendAsset(address, proxy_info.get, Conf.ergoAssets(assetId.toInt)).replaceAll("\"", "")
        if (txId.nonEmpty)
          paymentTokenDao.insert(TokenPayment(address, Conf.ergoAssets(assetId.toInt).assets("erg"), Conf.ergoAssets(assetId.toInt).name, txId))
        Ok(
        s"""{
           |  "txId": "${Conf.explorerFrontUrl}/en/transactions/${txId}"
           |}""".stripMargin
        ).as("application/json")
      }
    } catch {
      case e: WaitException => okException(e)
      case e: InvalidAddressException => medException(e)
      case e: InvalidRecaptchaException => medException(e)
      case e: Throwable => badException(e)
    }
  }
}
