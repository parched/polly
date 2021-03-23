type Data = List[Byte]

type Hash = List[Byte]

case class Block(prevHash: Hash, data: Data):
    def hash: Hash =
        HashUtils.sha256(prevHash ++ data)

type BlockChain = List[Block]

extension (chain: BlockChain)
    def isValid: Boolean =
        !chain.view.sliding(2).exists(pair => pair.head.hash != pair.last.prevHash)

    def addBlock(data: Data): BlockChain =
        val prevHash = if chain.isEmpty then List.empty else chain.last.hash
        chain :+ Block(prevHash, data)
