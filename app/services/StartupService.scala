package services

import akka.actor.{ActorRef, ActorSystem, Props}

import scala.concurrent.duration._
import javax.inject._
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.inject.ApplicationLifecycle
import slick.jdbc.JdbcProfile
import utils.{Client, Conf}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StartupService @Inject()(protected val dbConfigProvider: DatabaseConfigProvider, appLifecycle: ApplicationLifecycle,
                               system: ActorSystem, client: Client, balanceMonitoring: BalanceMonitoring)
                              (implicit ec: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {

  private val logger: Logger = Logger(this.getClass)

  val jobs: ActorRef = system.actorOf(Props(new Jobs(balanceMonitoring)), "scheduler")

  system.scheduler.scheduleAtFixedRate(
    initialDelay = 2.seconds,
    interval = Conf.monitorThreadInterval.seconds,
    receiver = jobs,
    message = Jobs.monitor
  )


  logger.info("App started!")
  client.setClient()
  client.setDAOs()

  appLifecycle.addStopHook { () =>
    logger.info("App stopped!")
    Future.successful(())
  }
}
