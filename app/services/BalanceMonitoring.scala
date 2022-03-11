package services

import javax.inject.{Inject, Singleton}
import utils.{Conf, Discord, Explorer}

@Singleton
class BalanceMonitoring @Inject ()(explorer: Explorer) {

  def monitor(): Unit = {
    Conf.proxyInfos.foreach(pk => {
      val lastBalance = explorer.getConfirmedBalanceFor(pk._1.toString)
      Conf.allAssets.foreach(asset => {
        var message = ""
        if (lastBalance.contains(asset._1)){
          if ((asset._2 * Conf.minNumberAsset) > lastBalance(asset._1)){
            message = s"Please charge asset ${asset._1} current amount is ${lastBalance(asset._1)}"
          }
        }
        else {
          message = s"Please charge asset ${asset._1} current amount is 0"
        }
        if (message.nonEmpty) Discord.sendMessageToWebHook(message)
      })
    })
  }

}
