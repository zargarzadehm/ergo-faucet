package utils

import javax.inject.Inject
import org.ergoplatform.appkit.impl.ErgoTreeContract
import org.ergoplatform.appkit.{Address, ErgoToken, InputBox, JavaHelpers, OutBox}
import Util._

import scala.collection.JavaConverters._
import play.api.Logger
import scorex.util.encode.Base16

import scala.collection.mutable

class CreateReward @Inject()(networkIObject: NetworkIObject, explorer: Explorer){
  private val logger: Logger = Logger(this.getClass)
  def sendErg(address: String, sendTransaction: Boolean = true): String = {
    validateAddress(address)
    var txId = ""
    def createProxyBox(proxyBox: (Long, Seq[InputBox], Seq[ErgoToken])): OutBox = {
      networkIObject.getCtxClient(implicit ctx => {
        val txB = ctx.newTxBuilder()
        var newProxyBox = txB.outBoxBuilder()
        newProxyBox = newProxyBox.value(proxyBox._1 - Conf.defaultAmount - Conf.defaultTxFee)
        if (proxyBox._3.nonEmpty) newProxyBox = newProxyBox.tokens(proxyBox._3: _*)
        newProxyBox.contract(new ErgoTreeContract(Conf.proxyAddress.getErgoAddress.script))
        newProxyBox.build()
      })
    }

    def createRewardBox(): OutBox = {
      networkIObject.getCtxClient(implicit ctx => {
        val txB = ctx.newTxBuilder()
        var newProxyBox = txB.outBoxBuilder()
        newProxyBox = newProxyBox.value(Conf.defaultAmount)
        newProxyBox.contract(new ErgoTreeContract(Address.create(address).getErgoAddress.script))
        newProxyBox.build()
      })
    }

    def calValue(inBoxes: Seq[InputBox]): (Long, Seq[InputBox], Seq[ErgoToken]) = {
      var inputValue: Long = 0
      var tokens: Seq[ErgoToken] = Seq()
      var boxes: Seq[InputBox] = Seq()
      for ( walletInput <- inBoxes) {
        inputValue += walletInput.getValue
        if (walletInput.getTokens.size() > 0) tokens = tokens ++ walletInput.getTokens.asScala
        boxes = boxes ++ Seq(walletInput)
        if (inputValue >= Conf.defaultAmount + Conf.defaultTxFee) return (inputValue, boxes, tokens)
      }
      (inputValue, boxes, tokens)
    }

    val boxes = networkIObject.getUnspentBox(Conf.proxyAddress)
    val unConfirmedBoxes = explorer.getUnconfirmedOutputsFor(Conf.proxyAddress.toString)
    val unConfirmedInputsBoxesIds = unConfirmedBoxes.map(_.id)
    val boxesVal = calValue(boxes.filter(box => {
      !unConfirmedInputsBoxesIds.contains(box.getId.toString)
    }))

    if (boxesVal._2.isEmpty) {
      logger.info(s"please wait and try later")
      throw new Throwable("please wait and try later")
    }
    else if (boxesVal._1 > 0){
     networkIObject.getCtxClient(implicit ctx => {
      val prover = ctx.newProverBuilder()
        .withDLogSecret(Conf.proxySecret)
        .build()
      val outputs: Seq[OutBox] = Seq(createProxyBox(boxesVal), createRewardBox())
      val txB = ctx.newTxBuilder()
      val tx = txB.boxesToSpend(boxesVal._2.asJava)
        .fee(Conf.defaultTxFee)
        .outputs(outputs: _*)
        .sendChangeTo(Conf.proxyAddress.getErgoAddress)
        .build()
      val signed = prover.sign(tx)
      logger.debug(s"reward data ${signed.toJson(false)}")
      txId = if (sendTransaction) ctx.sendTransaction(signed) else ""
      logger.info(s"sending reward tx ${txId}")
    })
    }
    else {
      logger.info(s"there is not enough Erg")
      throw new Throwable("there is not enough Erg")
    }
    txId
  }
  def sendDexToken(address: String, sendTransaction: Boolean = true): String = {
    validateAddress(address)
    var txId = ""
    def createProxyBox(proxyBox: InputBox): OutBox = {
      networkIObject.getCtxClient(implicit ctx => {
        val txB = ctx.newTxBuilder()
        var newProxyBox = txB.outBoxBuilder()
        newProxyBox = newProxyBox.value(proxyBox.getValue - Conf.assets("erg") - Conf.defaultTxFee)
        if (proxyBox.getTokens.asScala.nonEmpty) {
          val tokens = proxyBox.getTokens.asScala.map(token => {
            if (Conf.assets.get(token.getId.toString).isDefined)
            new ErgoToken(token.getId, token.getValue - Conf.assets(token.getId.toString))
            else token
          })
          newProxyBox = newProxyBox.tokens(tokens: _*)
        }
        newProxyBox.contract(new ErgoTreeContract(Conf.proxyAddress.getErgoAddress.script))
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

    def calValue(inBoxes: Seq[InputBox]): Option[InputBox] = {
      val selectedBox = inBoxes.find(box => {
        val inputAssets = mutable.Map.empty[String, Long]
        if (box.getTokens.size() > 0) {
          box.getTokens.asScala.foreach(token => inputAssets(token.getId.toString) = inputAssets.getOrElse(token.getId.toString, 0L) + token.getValue)
        }
        val ergCondition = box.getValue >= Conf.assets("erg") + Conf.defaultTxFee
        val assetCondition = Conf.assets.filterNot(x => x._1 == "erg").map(asset => {
          if (inputAssets.get(asset._1).isDefined) {
            if (inputAssets(asset._1) >= asset._2) true
            else false
          }
          else false
        }).reduce(_ && _)
        ergCondition && assetCondition
      })
      selectedBox
    }
    var boxToSpend: InputBox= null
    val boxes = networkIObject.getUnspentBox(Conf.proxyAddress)
    val unConfirmedBoxes = explorer.getUnconfirmedOutputsFor(Conf.proxyAddress.toString)
    val unConfirmedInputsBoxesIds = unConfirmedBoxes.map(_.id)
    val inp = boxes.filter(box => {
      !unConfirmedInputsBoxesIds.contains(box.getId.toString)
    })
    val selectedConfirmedBox = calValue(inp)

    if (selectedConfirmedBox.isEmpty) {
      val unConfirmedTxs = explorer.getUnconfirmedTransactionFor(Conf.proxyAddress.toString)
      val unConfirmedBoxes: Seq[InputBox] = unConfirmedTxs.flatMap(tx => {
        tx.getOutputsToSpend.asScala.filter(box => {
          Conf.addressEncoder.fromProposition(box.getErgoTree).get.toString == Conf.proxyAddress.toString
        })
      })
      val selectedUnConfirmedBox = calValue(unConfirmedBoxes)
      if (selectedUnConfirmedBox.isEmpty) {
        logger.info(s"there is not enough Erg or Token")
        throw new Throwable("please wait and try later")
      }
      else boxToSpend = selectedUnConfirmedBox.get
    }
    else {
      boxToSpend = selectedConfirmedBox.get
    }
     networkIObject.getCtxClient(implicit ctx => {
      val prover = ctx.newProverBuilder()
        .withDLogSecret(Conf.proxySecret)
        .build()
      val outputs: Seq[OutBox] = Seq(createProxyBox(boxToSpend), createRewardBox())
      val txB = ctx.newTxBuilder()
      val tx = txB.boxesToSpend(Seq(boxToSpend).asJava)
        .fee(Conf.defaultTxFee)
        .outputs(outputs: _*)
        .sendChangeTo(Conf.proxyAddress.getErgoAddress)
        .build()
      val signed = prover.sign(tx)
      logger.debug(s"reward data ${signed.toJson(false)}")
      txId = if (sendTransaction) ctx.sendTransaction(signed) else ""
      logger.info(s"sending reward tx ${txId} to ${address}")
    })
    txId
  }
}
