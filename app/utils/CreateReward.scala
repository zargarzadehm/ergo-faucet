package utils

import java.math.BigInteger

import javax.inject.Inject
import org.ergoplatform.appkit.impl.ErgoTreeContract
import org.ergoplatform.appkit.{Address, ErgoToken, InputBox, JavaHelpers, OutBox}
import Util._
import models.AssetConfig

import scala.collection.JavaConverters._
import play.api.Logger
import scorex.util.encode.Base16

import scala.collection.mutable

class CreateReward @Inject()(networkIObject: NetworkIObject, explorer: Explorer){
  private val logger: Logger = Logger(this.getClass)

  def sendAsset(address: String, proxy_info: (Address, BigInteger), assetConfig: AssetConfig, sendTransaction: Boolean = true): String = {
    validateAddress(address)
    var txId = ""
    def createProxyBox(proxyData: (Seq[InputBox], Long, mutable.Map[String, Long])): OutBox = {
      networkIObject.getCtxClient(implicit ctx => {
        val txB = ctx.newTxBuilder()
        var newProxyBox = txB.outBoxBuilder()
        newProxyBox = newProxyBox.value(proxyData._2 - assetConfig.assets("erg") - Conf.defaultTxFee)
        if (proxyData._3.nonEmpty) {
          var tokens: Seq[ErgoToken] = Seq.empty
          proxyData._3.foreach( token => {
            if (assetConfig.assets.contains(token._1)) {
              if ((token._2 - assetConfig.assets(token._1)) > 0)
                tokens = tokens :+ new ErgoToken(token._1, token._2 - assetConfig.assets(token._1))
            }
            else tokens =  tokens :+ new ErgoToken(token._1, token._2)
          })
          if (tokens.nonEmpty) newProxyBox = newProxyBox.tokens(tokens: _*)
        }
        newProxyBox.contract(new ErgoTreeContract(proxy_info._1.getErgoAddress.script, Conf.networkType))
        newProxyBox.build()
      })
    }

    def createRewardBox(): OutBox = {
      val tokens = assetConfig.assets.filterNot(_._1 == "erg").map(asset => new ErgoToken(asset._1, asset._2)).toSeq
      networkIObject.getCtxClient(implicit ctx => {
        val txB = ctx.newTxBuilder()
        var newProxyBox = txB.outBoxBuilder()
        newProxyBox = newProxyBox.value(assetConfig.assets("erg"))
        if (tokens.nonEmpty) newProxyBox = newProxyBox.tokens(tokens: _*)
        newProxyBox.contract(new ErgoTreeContract(Address.create(address).getErgoAddress.script, Conf.networkType))
        newProxyBox.build()
      })
    }

    def calValue(inBoxes: Seq[InputBox]): (Seq[InputBox], Long, mutable.Map[String, Long], Boolean) = {
      var totalInputValue: Long = 0
      val totalInputAssets = mutable.Map.empty[String, Long]
      var boxes: Seq[InputBox] = Seq()

      for ( walletInput <- inBoxes) {
        if (walletInput.getTokens.size() > 0) {
          walletInput.getTokens.asScala.foreach(token => totalInputAssets(token.getId.toString) = totalInputAssets.getOrElse(token.getId.toString, 0L) + token.getValue)
        }
        totalInputValue += walletInput.getValue
        boxes :+= walletInput
        val ergCondition = totalInputValue >= (assetConfig.assets("erg") + Conf.defaultTxFee + Conf.minErg)
        val assetsCondition= assetConfig.assets.filterNot(_._1.equals("erg")).map(asset => {
          if (totalInputAssets.contains(asset._1)) totalInputAssets(asset._1) >= asset._2
          else false
        }).reduceOption(_&&_)
        if (ergCondition && assetsCondition.getOrElse(true)) return (boxes, totalInputValue, totalInputAssets, true)
      }
      (Seq.empty, 0L, mutable.Map.empty[String, Long], false)
    }

    var selectedBox: (Seq[InputBox], Long, mutable.Map[String, Long]) = null
    var outBoxes: Seq[InputBox] = Seq.empty
    outBoxes = outBoxes ++ networkIObject.getUnspentBox(proxy_info._1)
    val unConfirmedBoxes = explorer.getUnconfirmedOutputsFor(proxy_info._1.toString)
    val unConfirmedInputsBoxesIds = unConfirmedBoxes.map(_.id)
    val boxesVal = calValue(outBoxes.filter(box => {
      !unConfirmedInputsBoxesIds.contains(box.getId.toString)
    }))
    if (!boxesVal._4) {
      outBoxes = Seq.empty
      var inBoxIds: Seq[String] = Seq.empty
      val unconfirmedTxs = explorer.getUnconfirmedTransactionFor(proxy_info._1.toString)
      unconfirmedTxs.foreach(tx => {
        if (Conf.addressEncoder.fromProposition(tx.getOutputsToSpend.get(0).getErgoTree).get.toString == proxy_info._1.toString) {
          val box = calValue(Seq(tx.getOutputsToSpend.get(0)))
          if (box._1.nonEmpty) {
            outBoxes = outBoxes ++ box._1
            inBoxIds = inBoxIds :+ tx.getSignedInputs.get(0).getId.toString
          }
        }
      })
      val noSpent = outBoxes.filterNot(box => inBoxIds.contains(box.getId.toString))
      if (noSpent.isEmpty) {
              logger.error(s"there is not enough Erg or Token for ${proxy_info._1}")
              throw new WaitException
      }
      else {
        val totalInputAssets = mutable.Map.empty[String, Long]
        if (noSpent.head.getTokens.size() > 0) {
          noSpent.head.getTokens.asScala.foreach(token => totalInputAssets(token.getId.toString) = totalInputAssets.getOrElse(token.getId.toString, 0L) + token.getValue)
        }
        selectedBox = (Seq(noSpent.head), noSpent.head.getValue, totalInputAssets)
      }
    }
    else selectedBox = (boxesVal._1, boxesVal._2, boxesVal._3)
    networkIObject.getCtxClient(implicit ctx => {
      val prover = ctx.newProverBuilder()
        .withDLogSecret(proxy_info._2)
        .build()
      var outputs: Seq[OutBox] = Seq(createProxyBox(selectedBox), createRewardBox())
      if (outputs.head.getValue == 0L) outputs = outputs.tail
      val txB = ctx.newTxBuilder()
      val tx = txB.boxesToSpend(selectedBox._1.asJava)
        .fee(Conf.defaultTxFee)
        .outputs(outputs: _*)
        .sendChangeTo(proxy_info._1.getErgoAddress)
        .build()
      val signed = prover.sign(tx)
      logger.debug(s"reward data ${signed.toJson(false)}")
      txId = if (sendTransaction) {
        try {
          ctx.sendTransaction(signed)
        }
        catch {
          case e: Throwable =>
            logger.error(s"error in sendTransaction $e")
            throw new WaitException
        }
      } else ""
      logger.info(s"sending asset ${assetConfig.name} with txId ${txId} from ${proxy_info._1.toString.substring(0, 6)} to ${address}")
    })
    txId
  }
}
