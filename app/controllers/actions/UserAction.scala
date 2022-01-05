package controllers.actions

import java.time.{LocalDateTime, ZoneOffset}

import javax.inject._
import models.{DiscordTokenObj, User}
import play.api.mvc._
import play.api.mvc.Results._
import utils.Util.{AuthException, NotVerifiedException}
import utils.{Discord, Util}

import scala.concurrent.{ExecutionContext, Future}

class UserRequest[A](val user: User, request: Request[A]) extends WrappedRequest[A](request)

class UserRequestOption[A](val user: Option[User], request: Request[A]) extends WrappedRequest[A](request)

trait BaseAction extends ActionBuilder[UserRequest, AnyContent]
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

trait BaseActionOptionUser extends ActionBuilder[UserRequestOption, AnyContent]
  with ActionTransformer[Request, UserRequestOption] {
  def transform[A](request: Request[A]): Future[UserRequestOption[A]] = Future.successful {
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

class UserAction @Inject()(parser: BodyParsers.Default)(implicit val ec: ExecutionContext)
  extends ActionBuilderImpl(parser)
  with BaseAction {

   override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] = {
    try {
      val DAOs = Util.DAOs.get
      var newSession: Session = Session.emptyCookie
      val sessionTokenOpt = request.session.get("access_token")

      val token = sessionTokenOpt
        .flatMap(token => DAOs._2.getSession(token))
        .map(token => (token.expires_in, token.refresh_token)).getOrElse(return Future.successful(Forbidden("You’re not logged in.")))

      if (LocalDateTime.now().isAfter(token._1)) {
        val discordToken = Discord.getTokenOauth(refresh_token = token._2).get
        newSession = request.session ++ DiscordTokenObj.unapply(discordToken)
      }
      else newSession= request.session

      block(request).map(_.withSession(newSession))
    }
    catch {
      case _: Throwable => throw new Util.AuthException
    }
  }
}

class UserActionOptional @Inject()(parser: BodyParsers.Default)(implicit val ec: ExecutionContext)
  extends ActionBuilderImpl(parser)
  with BaseActionOptionUser {

   override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] = {
    try {
      val DAOs = Util.DAOs.get
      var newSession: Session = request.session
      val sessionTokenOpt = request.session.get("access_token")

      val token = sessionTokenOpt
        .flatMap(token => DAOs._2.getSession(token))
        .map(token => (token.expires_in, token.refresh_token))

      if (token.isDefined) {
        val validToken = token.get
        if (LocalDateTime.now().isAfter(validToken._1)) {
        val discordToken = Discord.getTokenOauth(refresh_token = validToken._2).get
        newSession = request.session ++ DiscordTokenObj.unapply(discordToken)
        }
      }
      block(request).map(_.withSession(newSession))
    }
    catch {
      case _: Throwable => throw new Util.AuthException
    }
  }
}
