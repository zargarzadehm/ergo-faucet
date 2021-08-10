package dao

import javax.inject.{Inject, Singleton}
import models.Payment
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

trait PaymentErgComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._

  class PaymentErgTable(tag: Tag) extends Table[Payment](tag, "PAYMENT") {
    def address = column[String]("ADDRESS")
    def amount = column[Long]("AMOUNT")
    def txid = column[String]("TXID")
    def * = (address, amount, txid) <> (Payment.tupled, Payment.unapply)
  }
}

@Singleton()
class PaymentErgDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends PaymentErgComponent
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  val payments = TableQuery[PaymentErgTable]

  /**
   * inserts a Payment into db
   * @param pay Payment
   */
  def insert(pay: Payment): Future[Unit] = {
    db.run(payments += pay).map(_ => ())
  }

  /**
   * whether address exists
   * @param address in ergo network
   * @return boolean result
   */
  def exists(address: String): Boolean = {
    val res = db.run(payments.filter(_.address === address).exists.result)
    Await.result(res, 5.second)
  }

  /**
   * deletes all payments from db
   */
  def deleteAll(): Unit = {
    val res = db.run(payments.delete)
    Await.result(res, 5.second)
  }
}
