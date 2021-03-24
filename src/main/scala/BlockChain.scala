type Data = List[Byte]

type Hash = List[Byte]

case class Block(prevHash: Hash, data: Data):
    def hash: Hash =
        HashUtils.sha256(prevHash ++ data)

type BlockChain = List[Block]

extension (chain: BlockChain)
    def isValid: Boolean =
        !chain.view.sliding(2).exists(pair => pair.head.hash != pair.last.prevHash)

    def lastHash: Hash = if chain.isEmpty then List.empty else chain.last.hash

    def addData(data: Data): BlockChain =
        chain :+ Block(chain.lastHash, data)
