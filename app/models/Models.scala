package models

case class Payment(address: String, amount: Long, txid: String)
case class TokenPayment(address: String, amount: Long, typeTokens: String, txid: String)

case class Box(id: String, value: Long, address: String)


