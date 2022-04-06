package utils

import javax.inject.{Inject, Singleton}
import models.Box
import org.ergoplatform.appkit.SignedTransaction
import play.api.libs.json._
import scalaj.http.Http

import scala.collection.mutable

@Singleton
class Explorer @Inject()(networkIObject: NetworkIObject) {
  private val defaultHeader: Seq[(String, String)] = Seq[(String, String)](("Content-Type", "application/json"), ("Accept", "application/json"))

  def getUnconfirmedOutputsFor(address: String): Seq[Box] = try {
    val res = Http(s"${Conf.explorerUrl}/transactions/unconfirmed/byAddress/$address").headers(defaultHeader).asString
    if(res.body == ""){
      return Seq()
    }
    val js = Json.parse(res.body)
    var inputs: Seq[Box] = Seq()
    (js \ "items").as[Seq[JsValue]].foreach(tx => {
        inputs = inputs ++ (tx \ "inputs").as[Seq[JsValue]].map(box => {
          Box((box \ "id").as[String], (box \ "value").as[Long], (box \ "address").as[String])
        }).filter(_.address.equals(address))
    })
    inputs
  }

  def getConfirmedBalanceFor(address: String): mutable.Map[String, Long] = try {
    val res = Http(s"${Conf.explorerUrl}/api/v1/addresses/$address/balance/confirmed").headers(defaultHeader).asString
    val assets = mutable.Map.empty[String, Long]
    if(res.body == ""){
      return assets
    }
    val js = Json.parse(res.body)
    assets("erg") = (js \ "nanoErgs").as[Long]
    (js \ "tokens").as[Seq[JsValue]].foreach(token => {
      assets((token \ "tokenId").as[String]) = (token \ "amount").as[Long]
    })
    assets
  }

  def getUnconfirmedTransactionFor(address: String): Seq[SignedTransaction] = try {
    val res = Http(s"${Conf.explorerUrl}/transactions/unconfirmed/byAddress/$address").headers(defaultHeader).asString
    if(res.body == ""){
      return Seq.empty
    }
    networkIObject.getCtxClient(implicit ctx => {
    var unconfirmedTx: Seq[SignedTransaction] = Seq.empty
    val newJson = res.body.replaceAll("id", "boxId")
      .replaceAll("txId", "transactionId")
      .replaceAll("null", "\"\"")
    val js = Json.parse(newJson)
    if(js.toString() == ""){
      return Seq.empty
    }
    (js \ "items").as[Seq[JsValue]].foreach(tx => {
      val outStinrg = tx.toString().substring(0, 2) + "id" + tx.toString().substring(7)
      unconfirmedTx = unconfirmedTx :+ ctx.signedTxFromJson(outStinrg)
    })
     unconfirmedTx
    })
  }
}
