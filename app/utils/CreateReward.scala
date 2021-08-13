package utils

import java.math.BigInteger

import javax.inject.Inject
import org.ergoplatform.appkit.impl.ErgoTreeContract
import org.ergoplatform.appkit.{Address, ErgoToken, InputBox, OutBox}
import Util._

import scala.collection.JavaConverters._
import play.api.Logger

import scala.collection.mutable

class CreateReward @Inject()(networkIObject: NetworkIObject, explorer: Explorer){
  private val logger: Logger = Logger(this.getClass)
//  def sendErg(address: String, sendTransaction: Boolean = true): String = {
//    validateAddress(address)
//    var txId = ""
//    def createProxyBox(proxyBox: (Long, Seq[InputBox], Seq[ErgoToken])): OutBox = {
//      networkIObject.getCtxClient(implicit ctx => {
//        val txB = ctx.newTxBuilder()
//        var newProxyBox = txB.outBoxBuilder()
//        newProxyBox = newProxyBox.value(proxyBox._1 - Conf.defaultAmount - Conf.defaultTxFee)
//        if (proxyBox._3.nonEmpty) newProxyBox = newProxyBox.tokens(proxyBox._3: _*)
//        newProxyBox.contract(new ErgoTreeContract(Conf.proxyAddress.getErgoAddress.script))
//        newProxyBox.build()
//      })
//    }
//
//    def createRewardBox(): OutBox = {
//      networkIObject.getCtxClient(implicit ctx => {
//        val txB = ctx.newTxBuilder()
//        var newProxyBox = txB.outBoxBuilder()
//        newProxyBox = newProxyBox.value(Conf.defaultAmount)
//        newProxyBox.contract(new ErgoTreeContract(Address.create(address).getErgoAddress.script))
//        newProxyBox.build()
//      })
//    }
//
//    def calValue(inBoxes: Seq[InputBox]): (Long, Seq[InputBox], Seq[ErgoToken]) = {
//      var inputValue: Long = 0
//      var tokens: Seq[ErgoToken] = Seq()
//      var boxes: Seq[InputBox] = Seq()
//      for ( walletInput <- inBoxes) {
//        inputValue += walletInput.getValue
//        if (walletInput.getTokens.size() > 0) tokens = tokens ++ walletInput.getTokens.asScala
//        boxes = boxes ++ Seq(walletInput)
//        if (inputValue >= Conf.defaultAmount + Conf.defaultTxFee) return (inputValue, boxes, tokens)
//      }
//      (inputValue, boxes, tokens)
//    }
//
//    val boxes = networkIObject.getUnspentBox(Conf.proxyAddress)
//    val unConfirmedBoxes = explorer.getUnconfirmedOutputsFor(Conf.proxyAddress.toString)
//    val unConfirmedInputsBoxesIds = unConfirmedBoxes.map(_.id)
//    val boxesVal = calValue(boxes.filter(box => {
//      !unConfirmedInputsBoxesIds.contains(box.getId.toString)
//    }))
//
//    if (boxesVal._2.isEmpty) {
//      logger.info(s"please wait and try later")
//      throw new Throwable("please wait and try later")
//    }
//    else if (boxesVal._1 > 0){
//     networkIObject.getCtxClient(implicit ctx => {
//      val prover = ctx.newProverBuilder()
//        .withDLogSecret(Conf.proxySecret)
//        .build()
//      val outputs: Seq[OutBox] = Seq(createProxyBox(boxesVal), createRewardBox())
//      val txB = ctx.newTxBuilder()
//      val tx = txB.boxesToSpend(boxesVal._2.asJava)
//        .fee(Conf.defaultTxFee)
//        .outputs(outputs: _*)
//        .sendChangeTo(Conf.proxyAddress.getErgoAddress)
//        .build()
//      val signed = prover.sign(tx)
//      logger.debug(s"reward data ${signed.toJson(false)}")
//      txId = if (sendTransaction) ctx.sendTransaction(signed) else ""
//      logger.info(s"sending reward tx ${txId}")
//    })
//    }
//    else {
//      logger.info(s"there is not enough Erg")
//      throw new Throwable("there is not enough Erg")
//    }
//    txId
//  }
  def sendDexToken(address: String, proxy_info: (Address, BigInteger), sendTransaction: Boolean = true): String = {
    validateAddress(address)
    var txId = ""
    def createProxyBox(proxyBox: InputBox): OutBox = {
      networkIObject.getCtxClient(implicit ctx => {
        val txB = ctx.newTxBuilder()
        var newProxyBox = txB.outBoxBuilder()
        newProxyBox = newProxyBox.value(proxyBox.getValue - Conf.assets("erg") - Conf.defaultTxFee)
        if (proxyBox.getTokens.asScala.nonEmpty) {
          var tokens: Seq[ErgoToken] = Seq.empty
          proxyBox.getTokens.asScala.foreach( token => {
            if (Conf.assets.get(token.getId.toString).isDefined) {
              if ((token.getValue - Conf.assets(token.getId.toString)) > 0)
                tokens = tokens :+ new ErgoToken(token.getId, token.getValue - Conf.assets(token.getId.toString))
            }
            else tokens =  tokens :+ token
          })
          if (tokens.nonEmpty) newProxyBox = newProxyBox.tokens(tokens: _*)
        }
        newProxyBox.contract(new ErgoTreeContract(proxy_info._1.getErgoAddress.script))
        newProxyBox.build()
      })
    }

    def createRewardBox(): OutBox = {
      val tokens = Conf.assets.filterNot(_._1 == "erg").map(asset => new ErgoToken(asset._1, asset._2)).toSeq
      networkIObject.getCtxClient(implicit ctx => {
        val txB = ctx.newTxBuilder()
        var newProxyBox = txB.outBoxBuilder()
        newProxyBox = newProxyBox.value(Conf.assets("erg"))
        newProxyBox = newProxyBox.tokens(tokens: _*)
        newProxyBox.contract(new ErgoTreeContract(Address.create(address).getErgoAddress.script))
        newProxyBox.build()
      })
    }

    def calValue(inBoxes: Seq[InputBox]): (Long, Seq[InputBox], Seq[ErgoToken]) = {
      for ( walletInput <- inBoxes) {
        val inputAssets = mutable.Map.empty[String, Long]
        if (walletInput.getTokens.size() > 0) {
          walletInput.getTokens.asScala.foreach(token => inputAssets(token.getId.toString) = inputAssets.getOrElse(token.getId.toString, 0L) + token.getValue)
        }
        val ergCondition = walletInput.getValue >= (Conf.assets("erg") + Conf.defaultTxFee)
        val assetsCondition= Conf.assets.filterNot(_._1.equals("erg")).map(asset => {
          if (inputAssets.get(asset._1).isDefined) inputAssets(asset._1) >= asset._2
          else false
        }).reduce(_&&_)
        if (ergCondition && assetsCondition) return (walletInput.getValue, Seq(walletInput), walletInput.getTokens.asScala)
      }
      (0L, Seq.empty, Seq.empty)
    }
    var selectedBox: InputBox = null
    var outBoxes: Seq[InputBox] = Seq.empty
    outBoxes = outBoxes ++ networkIObject.getUnspentBox(proxy_info._1)
    val unConfirmedBoxes = explorer.getUnconfirmedOutputsFor(proxy_info._1.toString)
    val unConfirmedInputsBoxesIds = unConfirmedBoxes.map(_.id)
    val boxesVal = calValue(outBoxes.filter(box => {
      !unConfirmedInputsBoxesIds.contains(box.getId.toString)
    }))
    if (boxesVal._2.isEmpty) {
      outBoxes = Seq.empty
      var inBoxIds: Seq[String] = Seq.empty
      val unconfirmedTxs = explorer.getUnconfirmedTransactionFor(proxy_info._1.toString)
      unconfirmedTxs.foreach(tx => {
        if (Conf.addressEncoder.fromProposition(tx.getOutputsToSpend.get(0).getErgoTree).get.toString == proxy_info._1.toString) {
          val box = calValue(Seq(tx.getOutputsToSpend.get(0)))
          if (box._2.nonEmpty) {
            outBoxes = outBoxes ++ box._2
            inBoxIds = inBoxIds :+ tx.getSignedInputs.get(0).getId.toString
          }
        }
      })

      val noSpent = outBoxes.filterNot(box => inBoxIds.contains(box.getId.toString))
      if (noSpent.isEmpty) {
              logger.error(s"there is not enough Erg or Token for ${proxy_info._1}")
              throw new WaitException
      }
      else selectedBox = noSpent.head
    }
    else selectedBox = boxesVal._2.head
    networkIObject.getCtxClient(implicit ctx => {
      val prover = ctx.newProverBuilder()
        .withDLogSecret(proxy_info._2)
        .build()
      var outputs: Seq[OutBox] = Seq(createProxyBox(selectedBox), createRewardBox())
      if (outputs.head.getValue == 0L) outputs = outputs.tail
      val txB = ctx.newTxBuilder()
      val tx = txB.boxesToSpend(Seq(selectedBox).asJava)
        .fee(Conf.defaultTxFee)
        .outputs(outputs: _*)
        .sendChangeTo(proxy_info._1.getErgoAddress)
        .build()
      val signed = prover.sign(tx)
      logger.debug(s"reward data ${signed.toJson(false)}")
      txId = if (sendTransaction) {
        val result = ctx.sendTransaction(signed)
        if (result == null) throw new WaitException else result
      } else ""
      logger.info(s"sending reward tx ${txId} from ${proxy_info._1.toString.substring(0, 6)} to ${address}")
    })
    txId
  }
}
