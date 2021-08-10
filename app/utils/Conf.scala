package utils

import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.appkit.{Address, NetworkType}
import com.typesafe.config.ConfigFactory
import play.api.{Configuration, Logger}
import java.math.BigInteger

import scala.collection.mutable

object Conf {
  val config: Configuration = Configuration(ConfigFactory.load())
  private val logger: Logger = Logger(this.getClass)

  lazy val proxySecret: BigInteger = BigInt(readKey("proxy.secret"), 16).bigInteger
  lazy val proxyAddress: Address = Address.create(readKey("proxy.address"))
  lazy val nodeUrl: String = readKey("node.url").replaceAll("/$", "")
  lazy val networkType: NetworkType = if (readKey("node.networkType").toLowerCase.equals("mainnet")) NetworkType.MAINNET else NetworkType.TESTNET
  lazy val addressEncoder = new ErgoAddressEncoder(networkType.networkPrefix)
  lazy val explorerUrl: String = readKey("explorer.url-back").replaceAll("/$", "")
  lazy val explorerFrontUrl: String = readKey("explorer.url-front").replaceAll("/$", "")
  lazy val defaultAmount: Long = readKey("faucet.default", "60000000000L").toLong
  val ergAssetsConfig: Configuration = config.get[Configuration]("erg-dex-assets")
  lazy val assets = mutable.Map.empty[String, Long]
  ergAssetsConfig.keys.foreach(asset => assets(asset) = ergAssetsConfig.get[Long](asset))
  lazy val defaultTxFee: Long = readKey("fee.default", "1000000L").toLong

  def readKey(key: String, default: String = null): String = {
    try {
      if(config.has(key)) config.getOptional[String](key).getOrElse(default)
      else throw config.reportError(key,s"${key} not found!")
    } catch {
        case ex: Throwable =>
          logger.error(ex.getMessage)
          null
      }
  }
}
