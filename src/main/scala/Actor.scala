import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }

import akka.stream.*
import akka.stream.scaladsl.*

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.*
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.*

import scala.util.{ Try, Failure, Success }
import scala.concurrent.{ ExecutionContext, Future }

enum Message:
    case CreateBlock(data: Data)
    case GetBlocks(replyTo: ActorRef[BlockChain])
    case InsertBlock(index: Int, block: Block)
    case AddPeer(uri: Uri)
    case SetBlocks(blocks: BlockChain)

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol:

    implicit object BlockJsonFormat extends RootJsonFormat[Block]:
        import java.util.Base64

        val base64encoder = Base64.getEncoder()
        val base64decoder = Base64.getDecoder()

        def toBase64(data: List[Byte]): String =
            base64encoder.encodeToString(data.toArray)

        def fromBase64(data: String): List[Byte] =
            base64decoder.decode(data).toList

        def write(b: Block) =
            JsObject("prev_hash" -> JsString(toBase64(b.prevHash)), "data" -> JsString(toBase64(b.data)))

        def read(value: JsValue) = value.asJsObject.getFields("prev_hash", "data") match
            case Seq(JsString(prevHash), JsString(data)) => Block(fromBase64(prevHash), fromBase64(data))
            case _ => throw DeserializationException("Block expected")

    implicit val insertBlockFormat: RootJsonFormat[Message.InsertBlock] = jsonFormat2(Message.InsertBlock.apply)

object BlockChainActor:
    case class State(chain: BlockChain, peers: Set[Uri])

    def apply() = Behaviors.setup[Message] { context =>
        new BlockChainActor(context).next(State(Nil, Set.empty))
    }

class BlockChainActor private (context: ActorContext[Message]) extends JsonSupport:
    implicit val system: ActorSystem[Nothing] = context.system
    implicit val systemEc: ExecutionContext = system.executionContext

    def next(state: BlockChainActor.State): Behaviors.Receive[Message] = Behaviors.receiveMessage {
        case Message.CreateBlock(data) =>
            context.log.info(s"Block created")
            newChain(state, state.chain.addData(data))

        case Message.GetBlocks(replyTo) =>
            replyTo ! state.chain
            Behaviors.same

        case Message.InsertBlock(index, block) =>
            if index == state.chain.length && block.prevHash == state.chain.lastHash then
                context.log.info(s"Block inserted")
                newChain(state, state.chain :+ block)
            else if index < state.chain.length then
                Behaviors.same
            else // TODO: skip resolve if already doing it
                resolve(state)

        case Message.AddPeer(peer) =>
            context.log.info(s"Peer added: $peer")
            next(state.copy(peers = state.peers + peer))

        case Message.SetBlocks(blocks) =>
            context.log.info(s"Blockchain reset")
            newChain(state, blocks)
    }

    def resolve(state: BlockChainActor.State): Behaviors.Receive[Message] =
        val getBlocks: Uri => Future[BlockChain] = peer =>
            Http()
                .singleRequest(
                    HttpRequest(
                        method = HttpMethods.GET,
                        uri = peer.withPath(Uri.Path("/blocks"))
                    )
                )
                .flatMap(Unmarshal(_).to[BlockChain])

        val replyToSelf: ActorRef[Message] = context.self

        // longest valid chain is the simplest resolution
        Source(state.peers)
            // transform and ignore failures rather than failing the stream
            .mapAsyncUnordered(4)(getBlocks(_).transform(Success(_)))
            .collect {
                case Success(blocks) => blocks
            }
            .fold(state.chain)((currentLongest, next) =>
                if next.length > currentLongest.length && next.isValid then next else currentLongest
            )
            .runForeach(replyToSelf ! Message.SetBlocks(_))

        next(state)

    def newChain(state: BlockChainActor.State, newChain: BlockChain): Behaviors.Receive[Message] =
        newChain
            .lastOption
            .filterNot(state.chain.lastOption.contains(_)) // skip broadcast if it's the same
            .map(Message.InsertBlock(newChain.length - 1, _).toJson.toString)
            .foreach { block =>
                context.log.info(s"New last block: $block")

                state.peers.foreach(peer =>
                    Http()
                        .singleRequest(
                            HttpRequest(
                                method = HttpMethods.PUT,
                                uri = peer.withPath(Uri.Path("/block")),
                                entity = HttpEntity(ContentTypes.`application/json`, block)
                            )
                        )
                        .map(_.discardEntityBytes())
                )
            }

        next(state.copy(chain = newChain))
