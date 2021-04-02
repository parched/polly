import akka.actor.typed.scaladsl.AskPattern.*
import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.*
import akka.http.scaladsl.server.Directives.*
import akka.util.{ ByteString, Timeout }

import scala.concurrent.duration.*
import scala.concurrent.{ ExecutionContext, Future }
import scala.io.StdIn

import spray.json.enrichAny

object Main extends JsonSupport:
    def main(args: Array[String]): Unit =
        val port = args.lift(0).map(_.toInt).getOrElse(8080)

        implicit val system = ActorSystem(BlockchainActor(), "blockchain")
        // needed for the future flatMap/onComplete in the end
        implicit val executionContext = system.executionContext

        implicit val timeout: Timeout = 5.seconds

        val actor: ActorRef[Command] = system

        import Command.*

        val route = concat(
            (path("blocks") & get) {
                complete(actor.ask[Blockchain](GetBlocks(_)).map(_.toJson)) // TODO: what import is needed to avoid toJson?
            },
            (path("data") & post & entity(as[ByteString])) { data =>
                actor ! CreateBlock(data.toList)
                complete("creating block")
            }, // TODO: how about blocks/i for get and post
            (path("block") & put & entity(as[InsertBlock])) { block =>
                actor ! block
                complete("block inserted")
            },
            (path("peers") & put & entity(as[String])) { uri =>
                // TODO: validate as much a possible
                actor ! AddPeer(Uri(uri).withoutFragment) // discard other bits
                complete("peer inserted")
            }
        )

        val bindingFuture = Http().newServerAt("localhost", port).bind(route)

        println(s"Server online at http://localhost:${port}/\nPress RETURN to stop...")
        StdIn.readLine() // let it run until user presses return
        bindingFuture
            .flatMap(_.unbind()) // trigger unbinding from the port
            .onComplete(_ => system.terminate()) // and shutdown when done
