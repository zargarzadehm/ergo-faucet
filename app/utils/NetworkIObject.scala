package utils

import javax.inject.{Inject, Singleton}
import org.ergoplatform.appkit.{Address, BlockchainContext, ErgoClient, InputBox}

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
    getCtxClient(ctx =>
      ctx.getCoveringBoxesFor(address, (1e9*1e8).toLong, Seq.empty.asJava).getBoxes.asScala.toList
    )
  }

}
