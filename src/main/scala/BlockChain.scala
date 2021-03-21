type Data = List[Byte] // TODO: should this be properly typed?

type HashT = List[Byte] // TODO: sort out name conflict with Hash

case class Block(prevHash: HashT, data: Data):
    def hash: HashT =
        Hash.sha256(prevHash ++ data)

// TODO: should BlockChain be its own class? probably yes
object BlockChain:
    val genesisHash: HashT = List.empty

    def isValid(chain: List[Block]): Boolean =
        !chain.view.sliding(2).exists(pair => pair.head.hash != pair.last.prevHash)

    def addBlock(chain: List[Block], data: Data): List[Block] =
        val prevHash = if chain.isEmpty then genesisHash else chain.last.hash
        chain :+ Block(prevHash, data)
