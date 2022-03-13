package controllers.actions

import java.time.{LocalDateTime, ZoneOffset}
import scala.concurrent.{ExecutionContext, Future}
import javax.inject._
import play.api.mvc._

import models.User
import utils.Util.{AuthException, NotVerifiedException}
import utils.Util

class UserRequest[A](val user: User, request: Request[A]) extends WrappedRequest[A](request)

class UserAction @Inject() (val parser: BodyParsers.Default)(implicit val executionContext: ExecutionContext)
  extends ActionBuilder[UserRequest, AnyContent]
    with ActionTransformer[Request, UserRequest] {

  def transform[A](request: Request[A]): Future[UserRequest[A]] = Future.successful {
    val DAOs = Util.DAOs.get
    val sessionTokenOpt = request.session.get("access_token")
    val user = sessionTokenOpt
      .flatMap(token => DAOs._2.getSession(token))
      .filter(_.expires_in.isAfter(LocalDateTime.now(ZoneOffset.UTC)))
      .map(_.username)
      .flatMap(DAOs._1.getUser)
    val validUser = user.getOrElse(throw AuthException())
    if (!validUser.verified) throw NotVerifiedException()
    new UserRequest(validUser, request)
  }
}
