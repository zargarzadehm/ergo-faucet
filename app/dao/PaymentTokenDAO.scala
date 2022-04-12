package dao

import utils.Conf
import utils.Util.DuplicateRequestException

import java.sql.Timestamp
import java.time.LocalDateTime
import javax.inject.{Inject, Singleton}
import models.TokenPayment
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

trait PaymentTokenComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._

  implicit def TimestampColumnType = MappedColumnType.base[LocalDateTime,  Timestamp](
    dt => Timestamp.valueOf(dt),
    ts => ts.toLocalDateTime
  )

  class PaymentTokenTable(tag: Tag) extends Table[TokenPayment](tag, "TOKEN_PAYMENT") {
    def address = column[String]("ADDRESS")
    def erg_amount = column[Long]("ERG_AMOUNT")
    def type_tokens = column[String]("TYPE_TOKENS")
    def tx_id = column[String]("TXID")
    def username = column[String]("USERNAME")
    def ip = column[String]("IP")
    def created_time = column[LocalDateTime]("CREATED_TIME", O.Default(LocalDateTime.now()))
    def done = column[Boolean]("DONE", O.Default(false))
    def * = (username, address, erg_amount, type_tokens, ip, tx_id, created_time, done) <> (TokenPayment.tupled, TokenPayment.unapply)
    def user_token = index("USER_TOKEN", (username, type_tokens), unique = true)
  }

  class PaymentTokenTableArchive(tag: Tag) extends Table[TokenPayment](tag, "TOKEN_PAYMENT_ARCHIVE") {
    def address = column[String]("ADDRESS")
    def erg_amount = column[Long]("ERG_AMOUNT")
    def type_tokens = column[String]("TYPE_TOKENS")
    def tx_id = column[String]("TXID")
    def username = column[String]("USERNAME")
    def ip = column[String]("IP")
    def created_time = column[LocalDateTime]("CREATED_TIME")
    def done = column[Boolean]("DONE")
    def * = (username, address, erg_amount, type_tokens, ip, tx_id, created_time, done) <> (TokenPayment.tupled, TokenPayment.unapply)
    def user_token = index("USER_TOKEN_ARCHIVE", (username, type_tokens, created_time), unique = true)
  }

}

@Singleton()
class PaymentTokenDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends PaymentTokenComponent
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  val tokenPayments = TableQuery[PaymentTokenTable]
  val tokenPaymentsArchive = TableQuery[PaymentTokenTableArchive]

  /**
   * inserts a Payment into db
   * @param pay Payment
   */
  def insert(pay: TokenPayment): Unit = {
    Await.result(db.run(DBIO.seq(tokenPayments += pay)).map(_ => ()), Duration.Inf)
  }

  def insertConsiderOldPayment(pay: TokenPayment): Unit = {
    val filterOldPayQuery = tokenPayments.filter(payment => payment.username === pay.username && payment.type_tokens === pay.typeTokens)

    val insertConsiderOldPayment = for {
      pays <- filterOldPayQuery.exists.result
      _ <- {
        if (!pays) DBIO.seq(tokenPayments += pay)
        else throw DuplicateRequestException(s"You has already received ${pay.typeTokens} token type")
      }
    } yield { }
    Await.result(db.run(insertConsiderOldPayment), Duration.Inf)
  }

  def updatePayments(): Unit = {
    val filterOldPayQuery = tokenPayments.filter( _.created_time < LocalDateTime.now().minusDays(Conf.thresholdDayIgnorePayments))
    val updateTablesQuery = for {
      oldPay <- filterOldPayQuery.result
      _ <- DBIO.seq(tokenPaymentsArchive ++= oldPay, filterOldPayQuery.delete).transactionally
    } yield { }
    Await.result(db.run(updateTablesQuery), Duration.Inf)
  }

  /**
   * whether payment with username exists or not
   * @param username in Discord
   * @param type_tokens Type batch of assets
   * @return boolean result
   */
  def exists(username: String, address: String, ip: String, type_tokens: String): Boolean = {
    val res = db.run(tokenPayments.filter(payment => {
      (payment.address === address || payment.username === username || payment.ip === ip) && (payment.type_tokens === type_tokens)
    } ).exists.result)
    Await.result(res, Duration.Inf)
  }

  /**
   * deletes all payments from db
   */
  def deleteAll(): Unit = {
    val res = db.run(tokenPayments.delete)
    Await.result(res, Duration.Inf)
  }
}
