type Data = List[Byte] // TODO: should this be properly typed?

type HashT = List[Byte] // TODO: sort out name conflict with Hash

case class Block(hash: HashT, prevHash: HashT, data: Data)

// TODO: should BlockChain be its own class? probably yes
object BlockChain:
    val genesisHash: HashT = List.empty

    def calculateHash(prevHash: HashT, data: Data): HashT =
        Hash.sha256(prevHash ++ data)

    def verify(chain: List[Block]): Boolean =
        val isInvalidBlock = (block: Block, prevHash: HashT) =>
            prevHash != block.prevHash || calculateHash(block.prevHash, block.data) != block.hash
        val prevHashes = genesisHash +: chain.view.map(_.hash)
        !chain.view.zip(prevHashes).exists(isInvalidBlock.tupled)

    def addBlock(chain: List[Block], data: Data): List[Block] =
        val prevHash = if chain.isEmpty then genesisHash else chain.last.hash
        chain :+ Block(calculateHash(prevHash, data), prevHash, data)
