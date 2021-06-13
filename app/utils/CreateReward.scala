package utils

import javax.inject.Inject
import org.ergoplatform.appkit.impl.ErgoTreeContract
import org.ergoplatform.appkit.{Address, ErgoToken, InputBox, OutBox}

import scala.collection.JavaConverters._
import play.api.Logger

class CreateReward @Inject()(networkIObject: NetworkIObject, explorer: Explorer){
  private val logger: Logger = Logger(this.getClass)
  def sendTx(address: String, sendTransaction: Boolean = true): String = {
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
}
