import akka.actor.typed.ActorRef
import akka.http.scaladsl.model.Uri

enum Command:
    case CreateBlock(data: Data)
    case GetBlocks(replyTo: ActorRef[Blockchain])
    case InsertBlock(index: Int, block: Block)
    case AddPeer(uri: Uri)
    case SetBlocks(blocks: Blockchain)
