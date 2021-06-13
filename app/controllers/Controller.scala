package controllers

import akka.actor.ActorSystem
import dao.PaymentDAO
import javax.inject._
import models.Payment
import play.api.Logger
import play.api.mvc._
import utils.Util._
import utils.{Conf, CreateReward}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Controller @Inject()(payments: PaymentDAO, cc: ControllerComponents, actorSystem: ActorSystem, createReward: CreateReward)(implicit exec: ExecutionContext) extends AbstractController(cc) {

  private val logger: Logger = Logger(this.getClass)

  def exception(e: Throwable): Result = {
    logger.error(s"error in controller ${getStackTraceStr(e)}")
    BadRequest(s"""{"success": false, "message": "${e.getMessage}"}""").as("application/json")
  }

  /**
   * list of proposal related to a specific team
   */
  def payment(address: String): Action[AnyContent] = Action { implicit request =>
    try {
      if(payments.exists(address)) {
      BadRequest(
        s"""{
           |  "message": "This address has already received an ERG."
           |}""".stripMargin
      ).as("application/json")
      }
      else {
        val txId = createReward.sendTx(address).replaceAll("\"", "")
        payments.insert(Payment(address, Conf.defaultAmount, txId))
        Ok(
        s"""{
           |  "txId": "https://testnet.ergoplatform.com/en/transactions/${txId}"
           |}""".stripMargin
        ).as("application/json")
      }
    } catch {
      case e: Throwable => exception(e)
    }
  }
}
