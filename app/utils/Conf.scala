package utils

import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.appkit.{Address, NetworkType}
import com.typesafe.config.ConfigFactory
import play.api.{Configuration, Logger}
import java.math.BigInteger

import models.AssetConfig
import utils.Util.getStackTraceStr

import scala.collection.mutable

object Conf {
  val config: Configuration = Configuration(ConfigFactory.load())
  private val logger: Logger = Logger(this.getClass)

//  lazy val proxySecret: BigInteger = BigInt(readKey("proxy.secret"), 16).bigInteger
//  lazy val proxyAddress: Address = Address.create(readKey("proxy.address"))
  lazy val recaptchaKey: String = readKey("recaptcha-key", "")
  lazy val nodeUrl: String = readKey("node.url").replaceAll("/$", "")
  lazy val networkType: NetworkType = if (readKey("node.networkType").toLowerCase.equals("mainnet")) NetworkType.MAINNET else NetworkType.TESTNET
  lazy val addressEncoder = new ErgoAddressEncoder(networkType.networkPrefix)
  lazy val explorerUrl: String = readKey("explorer.url-back").replaceAll("/$", "")
  lazy val explorerFrontUrl: String = readKey("explorer.url-front").replaceAll("/$", "")

  var lastId = -1
  var ergoAssets = mutable.Map.empty[Int, AssetConfig]
  val ergoAssetsConfig: Configuration = config.get[Configuration]("ergo-assets")
  ergoAssetsConfig.subKeys.foreach(assetName => {
    val assetConfig: Configuration = ergoAssetsConfig.get[Configuration](assetName)
    val asName: String = assetConfig.get[String]("name")
    val ergoAssetsTmp = mutable.Map.empty[String, Long]
    assetConfig.keys.filterNot(name=> name.equals("name")).foreach(asset => ergoAssetsTmp(asset) = assetConfig.get[Long](asset))
    lastId += 1
    ergoAssets(lastId) = AssetConfig(asName, ergoAssetsTmp)
  })
//  try{
//
//
//  }
//  catch {
//      case e: Throwable =>
//        println("--------------------------------------------------------------------------------------------------------")
//        println(ergoAssetsConfig.subKeys)
//        getStackTraceStr(e)
//        println(e)
//  }
//  println(e.getMessage)


//  val ergoAssets = mutable.Map.empty[String, Long]
//  ergoAssetsConfig.keys.filterNot(name=> name.equals("name")).foreach(asset => ergoAssets(asset) = ergoAssetsConfig.get[Long](asset))
//
//  val ergAssetsConfig: Configuration = config.get[Configuration]("erg-asset")
//  lazy val ergAssets = mutable.Map.empty[String, Long]
//  ergAssetsConfig.keys.foreach(asset => ergAssets(asset) = ergAssetsConfig.get[Long](asset))
//
//  val ergDexAssetsConfig: Configuration = config.get[Configuration]("erg-dex-assets")
//  lazy val dexAssets = mutable.Map.empty[String, Long]
//  ergDexAssetsConfig.keys.foreach(asset => dexAssets(asset) = ergDexAssetsConfig.get[Long](asset))

  val ergoProxyConfig: Configuration = config.get[Configuration]("ergo-proxy")
  lazy val proxyInfos = mutable.Map.empty[Address, BigInteger]
  ergoProxyConfig.keys.foreach(info => proxyInfos(Address.create(info)) = BigInt(ergoProxyConfig.get[String](info), 16).bigInteger)

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
