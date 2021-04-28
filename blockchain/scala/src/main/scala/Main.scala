import akka.actor.typed.scaladsl.AskPattern.*
import akka.actor.typed.{ActorRef, Behavior, ActorSystem}
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

import BlockchainActor.{Command, PeerCommand}

object Main extends JsonSupport:
    def sendHttpRequestOnPeerCommand(
        uri: Uri
    )(using ActorSystem[Nothing], ExecutionContext) =
        Behaviors.receiveMessage[PeerCommand] {
            case block: PeerCommand.InsertBlock =>
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
                Behaviors.same

            case PeerCommand.GetBlocks(replyTo) =>
                Http()
                    .singleRequest(
                        HttpRequest(
                            method = HttpMethods.GET,
                            uri = uri.withPath(Uri.Path("/blocks"))
                        )
                    )
                    .flatMap(Unmarshal(_).to[Blockchain])
                    .foreach(replyTo ! _)
                Behaviors.same
        }

    def onUriCreatePeerAndAdd(
        addTo: ActorRef[BlockchainActor.Command]
    )(using ActorSystem[Nothing], ExecutionContext): Behavior[Uri] =
        Behaviors.receive[Uri]((context, uri) =>
            val peer =
                context.spawn(sendHttpRequestOnPeerCommand(uri), s"Peer @ $uri")
            addTo ! BlockchainActor.ControlCommand.AddPeer(peer)
            Behaviors.same
        )

    def startHttpServer(
        port: Int,
        blockchain: ActorRef[BlockchainActor.Command],
        peerSpawner: ActorRef[Uri]
    )(using ActorSystem[Nothing], ExecutionContext) =
        given timeout: Timeout = 5.seconds

        val route = concat(
            (path("blocks") & get) {
                complete(
                    // TODO: what import is needed to avoid toJson?
                    blockchain
                        .ask[Blockchain](
                            BlockchainActor.PeerCommand.GetBlocks(_)
                        )
                        .map(_.toJson)
                )
            },
            (path("data") & post & entity(as[ByteString])) { data =>
                blockchain ! BlockchainActor.ControlCommand.CreateBlock(
                    data.toList
                )
                complete("creating block")
            }, // TODO: how about blocks/i for get and post
            (path("block") & put & entity(
                as[BlockchainActor.PeerCommand.InsertBlock]
            )) { block =>
                blockchain ! block
                complete("block inserted")
            },
            (path("peers") & put & entity(as[String])) { uriRaw =>
                // TODO: validate as much a possible
                val uri = Uri(uriRaw).withoutFragment // discard other bits

                peerSpawner ! uri
                complete("peer inserted")
            }
        )

        Http()
            .newServerAt("localhost", port)
            .bind(route)
            .onComplete(_ =>
                println(s"Server online at http://localhost:${port}/")
            )

    def main(args: Array[String]): Unit =
        val port = args.lift(0).map(_.toInt).getOrElse(8080)

        val system = ActorSystem(
            Behaviors.setup[Uri](context =>
                given ActorSystem[Nothing] = context.system
                given ExecutionContext = context.system.executionContext

                val blockchainActor = context.spawn[BlockchainActor.Command](
                    BlockchainActor(),
                    "Blockchain"
                )
                startHttpServer(port, blockchainActor, context.self)
                onUriCreatePeerAndAdd(blockchainActor)
            ),
            "Gaurdian"
        )
