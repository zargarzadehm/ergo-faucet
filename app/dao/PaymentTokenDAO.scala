package dao

import javax.inject.{Inject, Singleton}
import models.TokenPayment
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

trait PaymentTokenComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._

  class PaymentTokenTable(tag: Tag) extends Table[TokenPayment](tag, "TOKEN_PAYMENT") {
    def address = column[String]("ADDRESS")
    def erg_amount = column[Long]("ERG_AMOUNT")
    def type_tokens = column[String]("TYPE_TOKENS")
    def txid = column[String]("TXID")
    def * = (address, erg_amount, type_tokens, txid) <> (TokenPayment.tupled, TokenPayment.unapply)
  }
}

@Singleton()
class PaymentTokenDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends PaymentTokenComponent
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  val tokenPayments = TableQuery[PaymentTokenTable]

  /**
   * inserts a Payment into db
   * @param pay Payment
   */
  def insert(pay: TokenPayment): Unit = {
    Await.result(db.run(DBIO.seq(tokenPayments += pay)).map(_ => ()), Duration.Inf)
  }

  /**
   * whether address exists
   * @param address in ergo network
   * @return boolean result
   */
  def exists(address: String, type_tokens: String): Boolean = {
    val res = db.run(tokenPayments.filter(payment => payment.address === address && payment.type_tokens === type_tokens).exists.result)
    Await.result(res, 5.second)
  }

  /**
   * deletes all payments from db
   */
  def deleteAll(): Unit = {
    val res = db.run(tokenPayments.delete)
    Await.result(res, 5.second)
  }
}
