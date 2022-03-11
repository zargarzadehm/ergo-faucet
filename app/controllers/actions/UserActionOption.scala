package controllers.actions
import models.{DiscordTokenObj, User}

import java.time.{LocalDateTime, ZoneOffset}
import utils.{Discord, Util}
import play.api.mvc._
import utils.Util.NotVerifiedException

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UserRequestOption[A](val user: Option[User], request: Request[A]) extends WrappedRequest[A](request)

class UserActionOption @Inject() (val parser: BodyParsers.Default)(implicit val executionContext: ExecutionContext)
  extends ActionBuilder[UserRequestOption, AnyContent]
    with ActionTransformer[Request, UserRequestOption] {

  def transform[A](request: Request[A]): Future[UserRequestOption[A]] = Future.successful {
    println("UserActionOption")
    val DAOs = Util.DAOs.get
    val sessionTokenOpt = request.session.get("access_token")

    val user = sessionTokenOpt
      .flatMap(token => DAOs._2.getSession(token))
      .filter(_.expires_in.isAfter(LocalDateTime.now(ZoneOffset.UTC)))
      .map(_.username)
      .flatMap(DAOs._1.getUser)
    if (user.isDefined) {
      if (!user.get.verified) throw NotVerifiedException()
    }
    new UserRequestOption(user, request)
  }

}
