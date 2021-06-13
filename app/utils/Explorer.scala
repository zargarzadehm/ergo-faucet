package utils

import javax.inject.Singleton
import models.Box
import play.api.libs.json._
import scalaj.http.Http

@Singleton
class Explorer {
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
}
