package utils
import play.api.Logger
import play.api.libs.json._
import scalaj.http.Http

import models.{DiscordToken, DiscordTokenObj, User, UserObj}


object Discord{
  private val defaultHeader: Seq[(String, String)] = Seq[(String, String)](("Content-Type", "application/x-www-form-urlencoded"), ("Accept", "application/json"))
  private val logger: Logger = Logger(this.getClass)

  def getTokenOauth(code: String = "", refresh_token: String = ""): Option[DiscordToken] = try {

    var data: Seq[(String, String)] = Seq.empty
    if (refresh_token.isEmpty) {
      data = Seq(
        "client_id" -> s"${Conf.discordConf.clientId}",
        "client_secret" -> s"${Conf.discordConf.clientSecret}",
        "grant_type" -> "authorization_code",
        "code" -> s"${code}",
        "redirect_uri" -> s"${Conf.discordConf.redirectUrl}"
      )
    }
    else {
      data = Seq(
        "client_id" -> s"${Conf.discordConf.clientId}",
        "client_secret" -> s"${Conf.discordConf.clientSecret}",
        "grant_type" -> "refresh_token",
        "refresh_token" -> refresh_token
      )
    }

    val res = Http(s"${Conf.discordConf.tokenUrl}").postForm(data).headers(defaultHeader).asString
    if(res.body == ""){
      return Option.empty
    }
    val js = Json.parse(res.body)
    Option(DiscordTokenObj(js))
  } catch {
      case e: Throwable =>
        logger.error(s"error in controller ${Util.getStackTraceStr(e)}")
        throw new Util.AuthException
  }

  def getUserData(discordToken: DiscordToken): Option[User] = try {
    val newHeaders = defaultHeader :+ ("Authorization", s"${discordToken.token_type} ${discordToken.access_token}")
    val res = Http(s"${Conf.discordConf.informationUrl}").headers(newHeaders).asString
    if(res.body == ""){
      return Option.empty
    }
    val js = Json.parse(res.body)

    UserObj(
      (js \ "id").as[String],
      (js \ "username").as[String],
      (js \ "discriminator").as[String],
      (js \ "email").as[String],
      (js \ "verified").as[Boolean]
    )
  } catch {
      case e: Throwable =>
        throw new Util.AuthException
  }

  def sendMessageToWebHook(message: String): Unit = {
    val webHookHeader: Seq[(String, String)] = Seq[(String, String)](("Accept", "application/json"), ("Content-Type", "application/json"))
    val content =
      s"""
         |{
         |  "username": "Faucet Announcement",
         |  "embeds": [
         |    {
         |      "author": {
         |        "name": "Admin"
         |      },
         |      "title": "${message.take(15).toUpperCase} ...",
         |      "color": ${BigInt(24, new scala.util.Random())},
         |      "fields": [
         |        {
         |          "name": "Message",
         |          "value": "$message"
         |        }
         |      ]
         |    }
         |  ]
         |}
         |""".stripMargin
    Http(s"${Conf.discordConf.webHookUrl}").postData(content).headers(webHookHeader).asString
  }



//  def getDiscordToken(session: Session): mutable.Map[String, String] = try {
//    val discordToken = mutable.Map.empty[String, String]
//    discordToken("access_token") = session("access_token")
//    discordToken("token_type") = session("token_type")
//    discordToken("expires_in") = session("expires_in")
//    discordToken("refresh_token") = session("refresh_token")
//    discordToken("scope") = session("scope")
//    discordToken
//  } catch {
//    case e: Throwable => throw new AuthException
//  }

}
