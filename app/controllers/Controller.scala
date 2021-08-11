package controllers

import akka.actor.ActorSystem
import dao._
import javax.inject._
import models.{Payment, TokenPayment}
import play.api.Logger
import play.api.mvc._
import utils.Util._
import utils.{Conf, CreateReward}

import scala.concurrent.ExecutionContext

@Singleton
class Controller @Inject()(paymentErgDao: PaymentErgDAO, paymentTokenDao: PaymentTokenDAO, cc: ControllerComponents, actorSystem: ActorSystem, createReward: CreateReward)(implicit exec: ExecutionContext) extends AbstractController(cc) {

  private val logger: Logger = Logger(this.getClass)

  def okException(e: Throwable): Result = {
    logger.info(s"error in controller ${e.getMessage}")
    Ok(s"""{"success": false, "message": "${e.getMessage}"}""").as("application/json")
  }
  def badException(e: Throwable): Result = {
    logger.error(s"error in controller ${getStackTraceStr(e)}")
    BadRequest(s"""{"success": false, "message": "${e.getMessage}"}""").as("application/json")
  }

//  /**
//   * Send erg
//   */
//  def ergPayment(address: String): Action[AnyContent] = Action { implicit request =>
//    try {
//      if(paymentErgDao.exists(address)) {
//      BadRequest(
//        s"""{
//           |  "message": "This address has already received an ERG."
//           |}""".stripMargin
//      ).as("application/json")
//      }
//      else {
//        val txId = createReward.sendErg(address).replaceAll("\"", "")
//        paymentErgDao.insert(Payment(address, Conf.defaultAmount, txId))
//        Ok(
//        s"""{
//           |  "txId": "${Conf.explorerFrontUrl}/en/transactions/${txId}"
//           |}""".stripMargin
//        ).as("application/json")
//      }
//    } catch {
//      case e: Throwable => exception(e)
//    }
//  }

  /**
   * Send Dex Token
   */
  def dexTokenPayment(address: String): Action[AnyContent] = Action { implicit request =>
    try {
      if(false) {
      BadRequest(
        s"""{
           |  "message": "This address has already received DEX Tokens."
           |}""".stripMargin
      ).as("application/json")
      }
      else {
        val proxy_info = selectRandomProxyInfo(Conf.proxyInfos)
        val txId = createReward.sendDexToken(address, proxy_info.get).replaceAll("\"", "")
        if (txId.nonEmpty)
          paymentTokenDao.insert(TokenPayment(address, Conf.assets("erg"), "DEX", txId))
        Ok(
        s"""{
           |  "txId": "${Conf.explorerFrontUrl}/en/transactions/${txId}"
           |}""".stripMargin
        ).as("application/json")
      }
    } catch {
      case e: WaitException => okException(e)
      case e: Throwable => badException(e)
    }
  }
}
