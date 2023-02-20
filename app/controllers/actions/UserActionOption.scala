package controllers.actions

import java.time.{LocalDateTime, ZoneOffset}
import play.api.mvc._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import models.User
import utils.{Conf, Util}
import utils.Util.NotVerifiedException

class UserRequestOption[A](val user: Option[User], request: Request[A]) extends WrappedRequest[A](request)

class UserActionOption @Inject()(val parser: BodyParsers.Default)(implicit val executionContext: ExecutionContext)
  extends ActionBuilder[UserRequestOption, AnyContent]
    with ActionTransformer[Request, UserRequestOption] {

    def transform[A](request: Request[A]): Future[UserRequestOption[A]] = Future.successful {
        if (Conf.discordConf.active) {
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
        new UserRequestOption(Option.empty, request)
    }

}
