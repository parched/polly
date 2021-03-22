import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.*
import akka.http.scaladsl.server.Directives.*
import akka.util.ByteString
import scala.io.StdIn

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.*

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol:
    import java.util.Base64

    val base64encoder = Base64.getEncoder()
    val base64decoder = Base64.getDecoder()
    def toBase64(data: List[Byte]): String =
        base64encoder.encodeToString(data.toArray)
    def fromBase64(data: String): List[Byte] =
        base64decoder.decode(data).toList

    implicit object BlockJsonFormat extends RootJsonFormat[Block]:
        def write(b: Block) =
            JsObject("prev_hash" -> JsString(toBase64(b.prevHash)), "data" -> JsString(toBase64(b.data)))

        def read(value: JsValue) = value.asJsObject.getFields("prev_hash", "data") match
            case Seq(JsString(prevHash), JsString(data)) =>
             Block(fromBase64(prevHash), fromBase64(data))
            case _ => throw DeserializationException("Block expected")


object Main extends JsonSupport:

    def main(args: Array[String]): Unit =
        val port = args.lift(0).map(_.toInt).getOrElse(8080)

        implicit val system = ActorSystem(Behaviors.empty, "my-system")
        // needed for the future flatMap/onComplete in the end
        implicit val executionContext = system.executionContext

        var blockChain = List.empty[Block]

        val route = concat(
            (path("blocks") & get) {
                complete(blockChain.toJson) // TODO: what import is needed to avoid toJson?
            },
            (path("data") & post & entity(as[ByteString])) { data =>
                blockChain = BlockChain.addBlock(blockChain, data.toList)
                complete("block created")
            }
        )

        val bindingFuture = Http().newServerAt("localhost", port).bind(route)

        println(s"Server online at http://localhost:${port}/\nPress RETURN to stop...")
        StdIn.readLine() // let it run until user presses return
        bindingFuture
            .flatMap(_.unbind()) // trigger unbinding from the port
            .onComplete(_ => system.terminate()) // and shutdown when done
