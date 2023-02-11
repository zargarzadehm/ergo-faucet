package utils

import dao.{SessionDAO, UserDAO}
import javax.inject.Inject
import org.ergoplatform.appkit.RestApiErgoClient
import play.api.Logger


class Client @Inject()(networkIObject: NetworkIObject, sessionDao: SessionDAO, userDAO: UserDAO) {
  private val logger: Logger = Logger(this.getClass)

  /**
   * Sets client for the entire app when the app starts
   *
   * @return current height of blockchain
   */
  def setClient(): Long = {
    try {
      networkIObject.client = RestApiErgoClient.create(
        Conf.nodeUrl,
        Conf.networkType,
        "",
        Conf.explorerUrl
      )
      networkIObject.getCtxClient(implicit ctx => {
        ctx.getHeight
      })

    } catch {
      case e: Throwable =>
        logger.error(s"Could not set client! ${e.getMessage}.")
        0L
    }
  }

  def setDAOs(): Unit = {
    Util.DAOs = Option((userDAO, sessionDao))
  }
}
