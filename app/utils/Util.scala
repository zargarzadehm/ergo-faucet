package utils

import java.io.{PrintWriter, StringWriter}
import java.math.BigInteger
import java.security.SecureRandom

import org.ergoplatform.appkit.Address

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

  def validateAddress(address: String): Boolean = {
    try{
      Conf.addressEncoder.fromString(address).get.script
      true
    } catch {
      case _: Throwable => throw new Throwable("Invalid withdraw address")
    }
  }
}
