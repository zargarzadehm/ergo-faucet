package utils

import javax.inject.{Inject, Singleton}
import models.Box
import org.ergoplatform.appkit.SignedTransaction
import play.api.libs.json._
import scalaj.http.Http

@Singleton
class Explorer@Inject()(networkIObject: NetworkIObject) {
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

  def getUnconfirmedTransactionFor(address: String): Seq[SignedTransaction] = try {
    val res = Http(s"${Conf.explorerUrl}/transactions/unconfirmed/byAddress/$address").headers(defaultHeader).asString
    if(res.body == ""){
      return Seq()
    }
    networkIObject.getCtxClient(implicit ctx => {
    var singedTxs: Seq[SignedTransaction] = Seq()
    val newJson = res.body.replaceAll("id", "boxId")
      .replaceAll("txId", "transactionId")
      .replaceAll("null", "\"\"")
    val js = Json.parse(newJson)
    (js \ "items").as[Seq[JsValue]].foreach(tx => {
      val outStinrg = tx.toString().substring(0, 2) + "id" + tx.toString().substring(7)
      singedTxs = singedTxs :+ ctx.signedTxFromJson(outStinrg)
    })
      singedTxs.reverse
    })
  }

}
