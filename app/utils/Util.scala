package utils

import java.io.{PrintWriter, StringWriter}
import java.math.BigInteger
import java.security.SecureRandom

import dao.{SessionDAO, UserDAO}
import org.ergoplatform.appkit.Address
import play.api.Logger
import play.api.libs.json._
import scalaj.http.Http

import scala.collection.mutable

object Util {

  var DAOs: Option[(UserDAO, SessionDAO)] = Option.empty

  def getStackTraceStr(e: Throwable): String = {
    val sw = new StringWriter
    val pw = new PrintWriter(sw)
    e.printStackTrace(pw)
    sw.toString
  }

  def selectRandomProxyInfo(seq: mutable.Map[Address, BigInteger]): Option[(Address, BigInteger)] = {
    val random = new SecureRandom()
    new scala.util.Random(random).shuffle(seq).headOption
  }

  final case class WaitException(private val message: String = "please wait and try later") extends Throwable(message)
  final case class InvalidAddressException(private val message: String = "Invalid withdraw address") extends Throwable(message)
  final case class InvalidRecaptchaException(private val message: String = "Invalid recaptcha") extends Throwable(message)
  final case class AuthException(private val message: String = "Authenticate Failed") extends Throwable(message)
  final case class NotVerifiedException(private val message: String = "Your discord account don't verified") extends Throwable(message)
  final case class DuplicateRequestException(private val message: String = s"This user has already received assets") extends Throwable(message)
  final case class NotValidIP(private val message: String = "Your IP is not valid") extends Throwable(message)

  def validateAddress(address: String): Boolean = {
    try{
      Conf.addressEncoder.fromString(address).get.script
      true
    } catch {
      case _: Throwable => throw new InvalidAddressException
    }
  }

  def verifyRecaptcha(challenge: String): Unit = {
    val logger: Logger = Logger(this.getClass)
    try{
      val defaultHeader: Seq[(String, String)] = Seq[(String, String)](("Content-Type", "application/json"), ("Accept", "application/json"))
      val res = Http(s"https://www.google.com/recaptcha/api/siteverify?secret=${Conf.recaptchaKey}&response=${challenge}").headers(defaultHeader).asString
      val js = Json.parse(res.body)
      if (!(js \ "success").as[Boolean]) {
        logger.info(s"response of google ${js}")
        throw new InvalidRecaptchaException
      }
    } catch {
      case _: InvalidRecaptchaException => throw new InvalidRecaptchaException
      case _: Throwable => throw new Throwable("problem in verify recaptcha")
    }
  }

}
