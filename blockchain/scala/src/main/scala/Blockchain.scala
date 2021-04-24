type Data = List[Byte]

type Hash = List[Byte]

extension (hash: Hash)
    def isValidBlockHash: Boolean =
        if hash.isEmpty then true
        else
            val asBigInt = BigInt(hash.toArray.reverse) // to big endian
            asBigInt >= 0 && asBigInt < Block.HashLimit

object Block:
    val HashLimit =
        BigInt(1) << (256 - 16) // Approx 1 second mine on gitpod instance

    def mine(
        prevHash: Hash,
        data: Data,
        shouldAbort: () => Boolean
    ): Option[Block] =
        Iterator
            .from(0)
            .takeWhile(_ => !shouldAbort()) // TODO: don't check this every time
            .map(Block(prevHash, data, _))
            .find(_.hash.isValidBlockHash)

case class Block(prevHash: Hash, data: Data, hashModifier: Int):

    def hash: Hash =
        HashUtils.sha256(
            prevHash
                ++ data
                :+ hashModifier.toByte
                :+ (hashModifier >> 8).toByte
                :+ (hashModifier >> 16).toByte
                :+ (hashModifier >> 24).toByte
        )

type Blockchain = List[Block]

extension (chain: Blockchain)
    def isValid: Boolean =
        // first prevHash must be empty
        chain.headOption.map(_.prevHash == List.empty).getOrElse(true) &&
            // all hashes must be valid
            chain.forall(
                _.prevHash.isValidBlockHash
            ) && lastHash.isValidBlockHash &&
            // all prevHashes must match
            (chain.length < 2 || !chain
                .sliding(2)
                .exists(pair => pair.head.hash != pair.last.prevHash))

    def lastHash: Hash = chain.lastOption.map(_.hash).getOrElse(List.empty)
