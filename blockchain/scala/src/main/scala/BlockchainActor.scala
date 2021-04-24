import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}

import akka.stream.*
import akka.stream.scaladsl.*

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.*
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.unmarshalling.Unmarshal

import spray.json.enrichAny

import scala.util.{Try, Failure, Success}
import scala.concurrent.{ExecutionContext, Future, Promise}

object BlockchainActor:
    case class State(
        chain: Blockchain,
        peers: Set[Uri],
        ourData: Set[Data],
        mineInProgress: Option[() => Unit]
    )

    def apply() = Behaviors.setup[Command] { context =>
        new BlockchainActor(context).next(
            State(Nil, Set.empty, Set.empty, None)
        )
    }

class BlockchainActor private (context: ActorContext[Command])
    extends JsonSupport:
    import BlockchainActor.*
    import Command.*

    implicit val system: ActorSystem[Nothing] = context.system
    implicit val systemEc: ExecutionContext = system.executionContext
    val replyToSelf: ActorRef[Command] = context.self

    def next(state: State): Behaviors.Receive[Command] =
        Behaviors.receiveMessage {
            case CreateBlock(data) =>
                if state.ourData.contains(data) then
                    context.log.info(s"Ignoring duplicate data")
                    Behaviors.same
                else
                    context.log.info(s"Creating new block")
                    val mineInProgress = state.mineInProgress.getOrElse {
                        context.log.info(s"Spawning miner due to data added")
                        spawnMiner(state.chain, data)
                    }
                    next(
                        state.copy(
                            ourData = state.ourData + data,
                            mineInProgress = Some(mineInProgress)
                        )
                    )

            case GetBlocks(replyTo) =>
                replyTo ! state.chain
                Behaviors.same

            case InsertBlock(index, block) =>
                if index < state.chain.length then
                    context.log.info(s"Ignoring too old block")
                    Behaviors.same
                else if !block.hash.isValidBlockHash then
                    context.log.info(s"Ignoring block with invalid hash")
                    Behaviors.same
                else if index > state.chain.length then
                    // TODO: skip resolve if already doing it
                    context.log.info(s"Received block too new so resolving")
                    resolve(state)
                    Behaviors.same
                else if block.prevHash != state.chain.lastHash then
                    context.log.info(
                        s"Ignoring block with invalid previous hash"
                    )
                    Behaviors.same
                else
                    context.log.info(s"Block inserted")
                    next(newChain(state, state.chain :+ block))

            case AddPeer(peer) =>
                context.log.info(s"Peer added: $peer")
                next(state.copy(peers = state.peers + peer))

            case SetBlocks(blocks) =>
                context.log.info(s"Blockchain reset")
                next(newChain(state, blocks))
        }

    def resolve(state: State): Unit =
        val getBlocks: Uri => Future[Blockchain] = peer =>
            Http()
                .singleRequest(
                    HttpRequest(
                        method = HttpMethods.GET,
                        uri = peer.withPath(Uri.Path("/blocks"))
                    )
                )
                .flatMap(Unmarshal(_).to[Blockchain])

        // longest valid chain is the simplest resolution
        Source(state.peers)
            // transform and ignore failures rather than failing the stream
            .mapAsyncUnordered(4)(getBlocks(_).transform(Success(_)))
            .collect { case Success(blocks) =>
                blocks
            }
            .fold(state.chain)((currentLongest, next) =>
                if next.length > currentLongest.length && next.isValid then next
                else currentLongest
            )
            .runForeach(replyToSelf ! SetBlocks(_))

    def newChain(state: State, newChain: Blockchain): State =
        newChain.lastOption
            // skip broadcast if it's the same
            .filterNot(state.chain.lastOption.contains(_))
            .map(InsertBlock(newChain.length - 1, _).toJson.toString)
            .foreach { block =>
                context.log.info(s"New last block: $block")

                state.peers.foreach(peer =>
                    Http()
                        .singleRequest(
                            HttpRequest(
                                method = HttpMethods.PUT,
                                uri = peer.withPath(Uri.Path("/block")),
                                entity = HttpEntity(
                                    ContentTypes.`application/json`,
                                    block
                                )
                            )
                        )
                        .map(_.discardEntityBytes())
                )
            }

        // cancel the mine in progress because it needs a new prevHash
        state.mineInProgress.foreach(cancel =>
            context.log.info(s"Cancelling miner due to chain modification")
            cancel()
        )
        // start a new one (this isn't a very optimal way for a big chain)
        val dataInChain = newChain.map(_.data).toSet
        val mineInProgress = state.ourData
            .find(!dataInChain.contains(_))
            .map(data =>
                context.log.info(s"Spawning miner due to chain modification")
                spawnMiner(newChain, data)
            )

        state.copy(chain = newChain, mineInProgress = mineInProgress)

    def spawnMiner(chain: Blockchain, data: Data): () => Unit =
        val cancel = Promise[Unit]
        // TODO: use a dedicated dispatcher or execution context
        Future {
            Block
                .mine(chain.lastHash, data, () => cancel.isCompleted)
                .foreach(replyToSelf ! InsertBlock(chain.length, _))
        }
        () => cancel.success(())
