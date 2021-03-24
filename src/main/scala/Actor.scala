import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors

enum Message:
    case AddData(data: Data)
    case GetBlocks(replyTo: ActorRef[BlockChain])

def next(chain: BlockChain): Behaviors.Receive[Message] = Behaviors.receive {
    case (_, Message.AddData(data)) => next(chain.addBlock(data))
    case (_, Message.GetBlocks(replyTo)) =>
        replyTo ! chain
        Behaviors.same
}
