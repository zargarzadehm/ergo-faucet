package utils

import java.io.{PrintWriter, StringWriter}
import java.math.BigInteger
import java.security.SecureRandom

import org.ergoplatform.appkit.Address
import play.api.libs.json._
import scalaj.http.Http

import scala.collection.mutable

object Util {

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

  def validateAddress(address: String): Boolean = {
    try{
      Conf.addressEncoder.fromString(address).get.script
      true
    } catch {
      case _: Throwable => throw new InvalidAddressException
    }
  }

  def verifyRecaptcha(challenge: String): Unit = {
    try{
      val defaultHeader: Seq[(String, String)] = Seq[(String, String)](("Content-Type", "application/json"), ("Accept", "application/json"))
      val data = s"""{
                    |  "secret": "${Conf.recaptchaKey}",
                    |  "response": "${challenge}"
                    |}""".stripMargin
      val res = Http(s"https://www.google.com/recaptcha/api/siteverify").postData(data).headers(defaultHeader).asString
      val js = Json.parse(res.body)
      if (!(js \ "success").as[Boolean]) throw new InvalidRecaptchaException
    } catch {
      case _: Throwable => throw new Throwable("problem in verify recaptcha")
    }
  }

}
