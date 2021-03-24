import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors

enum Message:
    case CreateBlock(data: Data)
    case GetBlocks(replyTo: ActorRef[BlockChain])
    case InsertBlock(index: Int, block: Block)

def next(chain: BlockChain): Behaviors.Receive[Message] = Behaviors.receive {
    case (_, Message.CreateBlock(data)) => next(chain.addData(data))
    case (_, Message.GetBlocks(replyTo)) =>
        replyTo ! chain
        Behaviors.same
    case (_, Message.InsertBlock(index, block)) =>
        if index == chain.length && block.prevHash == chain.lastHash then
            next(chain :+ block)
        else if index < chain.length then
            Behaviors.same
        else
            next(chain /* TODO consensus */)
}
