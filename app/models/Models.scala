package models

import java.time.LocalDateTime

import play.api.libs.json.JsValue
import play.api.mvc.Session

import scala.collection.mutable

case class Payment(address: String, amount: Long, txid: String)
case class TokenPayment(username: String, address: String, amount: Long, typeTokens: String, ip: String, txid: String, createdTime: LocalDateTime = LocalDateTime.now(), done: Boolean = false)
case class User(discordId: String, username: String, email: String, verified: Boolean)
object UserObj {
  def apply(discordId: String, username: String, discriminator: String, email: String, verified: Boolean): Option[User] ={
    try {
      Option(User(discordId, s"${username}#${discriminator}", email, verified))
    }
    catch {
      case _: Any =>  Option.empty
    }
  }
}
case class Box(id: String, value: Long, address: String)
case class DiscordToken(username: String, access_token: String, refresh_token: String, token_type: String,
                        expires_in: LocalDateTime, scope: String)
object DiscordTokenObj {
  def apply(session: Session, username: String): DiscordToken = {
    DiscordToken(
      username,
      session("access_token"),
      session("refresh_token"),
      session("token_type"),
      LocalDateTime.parse(session("expires_in")),
      session("scope")
    )
  }

  def apply(js: JsValue, username: String = ""): DiscordToken = {
    DiscordToken(
      username,
      (js \ "access_token").as[String],
      (js \ "refresh_token").as[String],
      (js \ "token_type").as[String],
      LocalDateTime.now().plusSeconds((js \ "expires_in").as[Long]),
      (js \ "scope").as[String]
    )
  }

  def unapply(arg: DiscordToken): mutable.Map[String, String] = {
    val discordTokenMap = mutable.Map.empty[String, String]
    discordTokenMap("access_token") = arg.access_token
    discordTokenMap("token_type") = arg.token_type
    discordTokenMap("expires_in") = arg.expires_in.toString
    discordTokenMap("refresh_token") = arg.refresh_token
    discordTokenMap("scope") = arg.scope
    discordTokenMap
  }

  def empty: DiscordToken = {
    DiscordToken("", "", "", "", LocalDateTime.MIN, "")
  }
}

case class AssetConfig(name: String, assets: mutable.Map[String, Long])
case class ButtonConfig(name: String, active: Boolean, url: String)

case class DiscordConfig(oauthLink: String, clientId: String, clientSecret: String, redirectUrl: String,
                         tokenUrl: String, informationUrl: String, webHookUrl: String)
