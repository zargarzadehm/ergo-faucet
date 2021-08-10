package utils

import java.io.{PrintWriter, StringWriter}
import java.math.BigInteger

import org.ergoplatform.appkit.{Address, JavaHelpers}
import sigmastate.basics.DLogProtocol.DLogProverInput

object Util {

  def getStackTraceStr(e: Throwable): String = {
    val sw = new StringWriter
    val pw = new PrintWriter(sw)
    e.printStackTrace(pw)
    sw.toString
  }
}
