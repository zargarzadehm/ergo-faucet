package services

import akka.actor.{Actor, ActorLogging}
import play.api.Logger

object Jobs {
  val monitor = "monitor"
}

class Jobs(balanceMonitoring: BalanceMonitoring)
  extends Actor with ActorLogging {
  private val logger: Logger = Logger(this.getClass)

  def receive = {
    case Jobs.monitor =>
      logger.info(s"Monitoring of Faucet balance started")
      balanceMonitoring.monitor()
      logger.info("Monitoring of Faucet balance done")
  }

}
