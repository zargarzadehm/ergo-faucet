package utils

import org.ergoplatform.appkit.BoxOperations.ExplorerApiUnspentLoader

import javax.inject.{Inject, Singleton}
import org.ergoplatform.appkit.{Address, BlockchainContext, BoxOperations, ErgoClient, InputBox}

import scala.collection.JavaConverters._

@Singleton
class NetworkIObject @Inject()() {
  var client: ErgoClient = _

  def getCtxClient[T](f: BlockchainContext => T): T = {
    client.execute { ctx =>
      f(ctx)
    }
  }

  /**
   * @return current height of the blockchain
   */
  def getHeight: Long = {
    getCtxClient(ctx => ctx.getHeight)
  }

  /**
   * @param address :Address get a valid address
   * @return List of input address boxes
   */
  def getUnspentBox(address: Address): List[InputBox] = {
    getCtxClient(ctx => {
      val maxErg = (1e9 * 1e8).toLong
      val inputBoxesLoader = new ExplorerApiUnspentLoader()
      inputBoxesLoader.prepare(ctx, List(address).asJava, maxErg, Seq.empty.asJava)
      val coverBoxes = BoxOperations.getCoveringBoxesFor(
        maxErg,
        Seq.empty.asJava,
        false,
        (page: Integer) => inputBoxesLoader.loadBoxesPage(ctx, address, page)
      )
      coverBoxes.getBoxes.asScala.toList
    })
  }

}
