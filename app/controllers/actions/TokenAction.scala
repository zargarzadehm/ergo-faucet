package controllers.actions
import models.DiscordTokenObj

import java.time.LocalDateTime
import utils.{Discord, Util}
import play.api.mvc._
import play.filters.csrf.CSRF

import javax.inject.Inject
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class TokenAction @Inject() (parser: BodyParsers.Default)(implicit ec: ExecutionContext)
  extends ActionBuilderImpl(parser) {
  override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
    try {
      val DAOs = Util.DAOs.get
      var newSession: Session = request.session

      val token = request.session.get("access_token")
        .flatMap(token => DAOs._2.getSession(token))
        .map(token => (token.username, token.expires_in, token.refresh_token))

      val user = token
        .map(_._1)
        .flatMap(DAOs._1.getUser)

      if (user.isDefined && token.isDefined) {
        val validToken = token.get
        if (LocalDateTime.now().isAfter(validToken._2)) {
          val discordToken = Discord.getTokenOauth(refresh_token = validToken._3).get
          DAOs._2.insertUserSession(discordToken, user.get)
          val csrfToken = CSRF.getToken(request).get.value
          newSession = Session.apply(DiscordTokenObj.unapply(discordToken).toMap) ++ mutable.Map("csrfToken" -> csrfToken)
        }
      }
      block(request).map(_.withSession(newSession))
    }
    catch {
      case _: Throwable => throw new Util.AuthException
    }
  }
}
