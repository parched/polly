import akka.actor.typed.scaladsl.AskPattern.*
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.*
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.unmarshalling.Unmarshal

import spray.json.enrichAny

import akka.util.{ByteString, Timeout}

import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn

import spray.json.enrichAny

import BlockchainActor.Command

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
                complete(
                    // TODO: what import is needed to avoid toJson?
                    actor.ask[Blockchain](GetBlocks(_)).map(_.toJson)
                )
            },
            (path("data") & post & entity(as[ByteString])) { data =>
                actor ! CreateBlock(data.toList)
                complete("creating block")
            }, // TODO: how about blocks/i for get and post
            (path("block") & put & entity(as[InsertBlock])) { block =>
                actor ! block
                complete("block inserted")
            },
            (path("peers") & put & entity(as[String])) { uriRaw =>
                // TODO: validate as much a possible
                val peer = new BlockchainActor.Peer {
                    val uri = Uri(uriRaw).withoutFragment // discard other bits

                    override def notifyNewBlock(
                        block: Command.InsertBlock
                    ): Unit =
                        Http()
                            .singleRequest(
                                HttpRequest(
                                    method = HttpMethods.PUT,
                                    uri = uri.withPath(Uri.Path("/block")),
                                    entity = HttpEntity(
                                        ContentTypes.`application/json`,
                                        block.toJson.toString
                                    )
                                )
                            )
                            .map(_.discardEntityBytes())

                    override def getBlocks(): Future[Blockchain] =
                        Http()
                            .singleRequest(
                                HttpRequest(
                                    method = HttpMethods.GET,
                                    uri = uri.withPath(Uri.Path("/blocks"))
                                )
                            )
                            .flatMap(Unmarshal(_).to[Blockchain])
                }

                actor ! AddPeer(peer)
                complete("peer inserted")
            }
        )

        Http()
            .newServerAt("localhost", port)
            .bind(route)
            .onComplete(_ =>
                println(s"Server online at http://localhost:${port}/")
            )
