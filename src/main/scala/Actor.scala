import akka.actor.typed.ActorRef
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
import scala.concurrent.Future

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

object BlockChainActor extends JsonSupport:
    case class State(chain: BlockChain, peers: Set[Uri])

    def apply() = next(State(Nil, Set.empty))

    def next(state: State): Behaviors.Receive[Message] = Behaviors.receive {
        case (ctx, Message.CreateBlock(data)) =>
            val newChain = state.chain.addData(data)
            val block = Message.InsertBlock(newChain.length - 1, newChain.last).toJson.toString
            ctx.log.info(s"Block created: $block")

            // broadcast it
            implicit val system = ctx.system
            implicit val executionContext = system.executionContext
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

            next(state.copy(chain = newChain))

        case (_, Message.GetBlocks(replyTo)) =>
            replyTo ! state.chain
            Behaviors.same

        case (ctx, Message.InsertBlock(index, block)) =>
            if index == state.chain.length && block.prevHash == state.chain.lastHash then
                ctx.log.info(s"Block inserted at $index: $block")
                next(state.copy(chain = state.chain :+ block))
            else if index < state.chain.length then
                Behaviors.same
            else // TODO: skip resolve if already doing it
                resolve(ctx, state)

        case (ctx, Message.AddPeer(peer)) =>
            ctx.log.info(s"Peer added: $peer")
            next(state.copy(peers = state.peers + peer))

        case (ctx, Message.SetBlocks(blocks)) =>
            ctx.log.info(s"Blockchain reset: $blocks")
            next(state.copy(chain = blocks))
    }

    def resolve(ctx: ActorContext[Message], state: State): Behaviors.Receive[Message] =
        implicit val system = ctx.system
        implicit val executionContext = system.executionContext
        val getBlocks: Uri => Future[Try[BlockChain]] = peer =>
            Http()
                .singleRequest(
                    HttpRequest(
                        method = HttpMethods.GET,
                        uri = peer.withPath(Uri.Path("/blocks"))
                    )
                )
                .flatMap(Unmarshal(_).to[BlockChain])
                .transform(Success(_))

        val replyToSelf: ActorRef[Message] = ctx.self

        // longest valid chain is the simplest resolution
        Source(state.peers)
            .mapAsyncUnordered(4)(getBlocks)
            .collect {
                case Success(blocks) => blocks
            }
            // the length filter isn't strictly necessary but it saves
            // validating the whole chain if not needed
            .filter(chain => chain.length > state.chain.length && chain.isValid)
            .fold(state.chain)((currentLongest, next) => if next.length > currentLongest.length then next else currentLongest)
            .runForeach(replyToSelf ! Message.SetBlocks(_))

        next(state)