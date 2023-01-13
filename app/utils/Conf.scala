package utils

import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.appkit.{Address, NetworkType}
import com.typesafe.config.ConfigFactory
import play.api.{Configuration, Logger}
import java.math.BigInteger

import models.{AssetConfig, ButtonConfig, DiscordConfig}

import scala.collection.mutable

object Conf {
  val config: Configuration = Configuration(ConfigFactory.load())
  private val logger: Logger = Logger(this.getClass)

  lazy val recaptchaKey: String = readKey(config, "recaptcha-key", "")
  lazy val siteKey: String = readKey(config, "site-key", "")

  private val discordConfig: Configuration = config.get[Configuration]("discord")
  lazy val discordConf: DiscordConfig = DiscordConfig(
    readKey(discordConfig, "oauth-link"),
    readKey(discordConfig, "client-id"),
    readKey(discordConfig, "client-secret"),
    readKey(discordConfig, "redirect-uri"),
    readKey(discordConfig, "get-token-url"),
    readKey(discordConfig, "get-info-url"),
    readKey(discordConfig, "web-hook-url"),
  )

  lazy val mainButton: String = readKey(config, "main-button", "")
  lazy val title: String = readKey(config, "title", "")

  var buttons: Seq[ButtonConfig] = Seq.empty
  val buttonsConfig: Configuration = config.get[Configuration]("buttons")
  buttonsConfig.subKeys.foreach(buttonName => {
    val buttonConfig: Configuration = buttonsConfig.get[Configuration](buttonName)
    val name: String = readKey(buttonConfig,"name")
    val url: String = readKey(buttonConfig,"url")
    val active: Boolean = readKey(buttonConfig,"active").toBoolean
    buttons = buttons :+ ButtonConfig(name, active, url)
  })

  lazy val nodeUrl: String = readKey(config, "node.url").replaceAll("/$", "")
  lazy val networkType: NetworkType = if (readKey(config, "node.networkType").toLowerCase.equals("mainnet")) NetworkType.MAINNET else NetworkType.TESTNET
  lazy val addressEncoder = new ErgoAddressEncoder(networkType.networkPrefix)
  lazy val explorerUrl: String = readKey(config, "explorer.url-back").replaceAll("/$", "")
  lazy val explorerFrontUrl: String = readKey(config, "explorer.url-front").replaceAll("/$", "")

  var lastId: Int = -1
  var ergoAssets = mutable.Map.empty[Int, AssetConfig]
  var allAssets = mutable.Map.empty[String, Long]
  private val ergoAssetsConfig: Configuration = config.get[Configuration]("ergo-assets")
  ergoAssetsConfig.subKeys.foreach(assetName => {
    val assetConfig: Configuration = ergoAssetsConfig.get[Configuration](assetName)
    val asName: String = readKey(assetConfig, "name")
    val ergoAssetsTmp = mutable.Map.empty[String, Long]
    assetConfig.keys.filterNot(name=> name.equals("name")).foreach(asset => {
      ergoAssetsTmp(asset) = readKey(assetConfig, asset).toLong
      if (allAssets.contains(asset)) {
        allAssets(asset) = Math.max(allAssets(asset), readKey(assetConfig, asset).toLong)
      }
      else {
        allAssets(asset) = readKey(assetConfig, asset).toLong
      }
    })
    lastId += 1
    ergoAssets(lastId) = AssetConfig(asName, ergoAssetsTmp)
  })

  private val ergoProxyConfig: Configuration = config.get[Configuration]("ergo-proxy")
  lazy val proxyInfos = mutable.Map.empty[Address, BigInteger]
  ergoProxyConfig.keys.foreach(info => proxyInfos(Address.create(info)) = BigInt(readKey(ergoProxyConfig, info), 16).bigInteger)

  lazy val defaultTxFee: Long = readKey(config, "fee.default", "1000000L").toLong
  lazy val minErg: Long = 100000L
  lazy val minNumberAsset: Int = readKey(config, "min-number-asset", "30").toInt
  lazy val monitorThreadInterval: Int = readKey(config, "monitorThreadInterval", "1800").toInt
  lazy val paymentMonitorThreadInterval: Long = readKey(config, "paymentMonitorThreadInterval", "86400").toLong
  lazy val thresholdDayIgnorePayments: Long = readKey(config, "thresholdDayIgnorePayments", "5").toLong

  lazy val ipField: String = readKey(config, "ip-field", "cf-connecting-ip")

  def readKey(config: Configuration, key: String, default: String = null): String = {
    try {
      if(config.has(key)) config.getOptional[String](key).get
      else if(default.nonEmpty) default
      else throw config.reportError(key,s"$key not found!")
    } catch {
        case ex: Throwable =>
          logger.error(ex.getMessage)
          null
      }
  }
}
