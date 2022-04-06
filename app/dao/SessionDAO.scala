package dao

import java.sql.Timestamp

import javax.inject.{Inject, Singleton}
import models.{DiscordToken, User}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import java.time.LocalDateTime

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

trait SessionComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._

  implicit def TimestampColumnType = MappedColumnType.base[LocalDateTime,  Timestamp](
    dt => Timestamp.valueOf(dt),
    ts => ts.toLocalDateTime
  )


  class SessionTable(tag: Tag) extends Table[DiscordToken](tag, "SESSION") {
    def username = column[String]("USERNAME", O.PrimaryKey)
    def access_token = column[String]("ACCESS_TOKEN")
    def refresh_token = column[String]("REFRESH_TOKEN")
    def token_type = column[String]("TOKEN_TYPE")
    def expires_in = column[LocalDateTime]("EXPIRES_IN")
    def scope = column[String]("SCOPE")
    def txToken = index("USER_TOKEN", (username, access_token), unique = true)

    def * = (username, access_token, refresh_token, token_type, expires_in, scope) <> (DiscordToken.tupled, DiscordToken.unapply)
  }
}

@Singleton()
class SessionDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends SessionComponent with UserComponent
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  val sessions = TableQuery[SessionTable]
  val users = TableQuery[UserTable]


  def getSession(token: String): Option[DiscordToken] = {
    Await.result(db.run(sessions.filter(session => session.access_token === token).result.headOption), Duration.Inf)
  }

  /**
   * inserts a session into db
   * @param token Session
   */
  def insertUserSession(token: DiscordToken, user: User): Unit = {
    val filterOldUserQuery = users.filter(obj => obj.username === user.username)
    val filterOldSessionQuery = sessions.filter(session => session.username === token.username &&
      session.access_token ===  token.access_token)


    val insertConsiderOldSession = for {
      existUser <- filterOldUserQuery.exists.result
      existSession <- filterOldSessionQuery.exists.result
      _ <- {
        if (existUser) {
          if (!existSession) {
            DBIO.seq(sessions += token)
          }
          else {
            DBIO.seq(filterOldSessionQuery.map(token => (token.access_token, token.refresh_token, token.expires_in)).update(token.access_token, token.refresh_token, token.expires_in).map(_ => ()))
          }
        }
        else {
          DBIO.seq(users += user, sessions += token).transactionally
        }
      }
    } yield { }

    Await.result(db.run(insertConsiderOldSession.transactionally), Duration.Inf)
  }

  /**
   * whether username exists
   * @param username in discord
   * @return boolean result
   */
  def exists(username: String, accessToken: String): Boolean = {
    val res = db.run(sessions.filter(session => session.username === username && session.access_token === accessToken).exists.result)
    Await.result(res, 5.second)
  }

  /**
   * deletes all sessions from db
   */
  def deleteAll(): Unit = {
    val res = db.run(sessions.delete)
    Await.result(res, 5.second)
  }
}
