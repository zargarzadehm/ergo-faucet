package dao

import javax.inject.{Inject, Singleton}
import models.User
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

trait UserComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._

  class UserTable(tag: Tag) extends Table[User](tag, "USER") {
    def discordId = column[String]("DISCORD_ID")
    def username = column[String]("USERNAME", O.PrimaryKey)
    def email = column[String]("EMAIL")
    def verified = column[Boolean]("VERIFIED")
    def * = (discordId, username, email, verified) <> (User.tupled, User.unapply)
  }
}

@Singleton()
class UserDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends UserComponent
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  val users = TableQuery[UserTable]

  def getUser(username: String): Option[User] = {
    Await.result(db.run(users.filter(user => user.username === username).result.headOption), Duration.Inf)
  }

  /**
   * inserts a User into db
   * @param user User
   */
  def insert(user: User): Unit = {
    Await.result(db.run(DBIO.seq(users += user)).map(_ => ()), Duration.Inf)
  }

}
