package utils

import java.io.{PrintWriter, StringWriter}

object Util {

  def getStackTraceStr(e: Throwable): String = {
    val sw = new StringWriter
    val pw = new PrintWriter(sw)
    e.printStackTrace(pw)
    sw.toString
  }

  def validateAddress(address: String): Boolean = {
    try{
      Conf.addressEncoder.fromString(address).get.script
      true
    } catch {
      case _: Throwable => throw new Exception("Invalid withdraw address")
    }
  }
}
