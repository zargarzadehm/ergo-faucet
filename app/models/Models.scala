package models

import scala.collection.mutable

case class Payment(address: String, amount: Long, txid: String)
case class TokenPayment(address: String, amount: Long, typeTokens: String, txid: String)

case class Box(id: String, value: Long, address: String)

case class AssetConfig(name: String, assets: mutable.Map[String, Long])
case class ButtonConfig(name: String, active: Boolean, url: String)
