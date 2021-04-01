import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.Assertions.*

class BlockchainSpec extends AnyFlatSpec {
    val someHash: Hash = List(1, 2, 3, 4)
    val someData: Data = List(5, 6, 7)
    val someBlockWithEmptyPreviousHashAndValidHash: Block =
        Block(List(), List(1, 2), 14293)
    val someBlockWithEmptyPreviousHashAndInvalidHash: Block =
        Block(List(), List(1, 2), 123)
    val someBlockWithNonEmptyPreviousHashAndValidHash: Block =
        Block(List(1), List(1, 2), 253274)

    "A Hash (that is empty)" should "have a valid block hash" in {
        assert(List.empty[Byte].isValidBlockHash)
    }

    "Block.mine (with shouldAbort always true)" should "return None" in {
        assert(Block.mine(someHash, someData, () => true).isEmpty)
    }

    "Block.mine (with shouldAbort always false)" should "return Some" in {
        assert(Block.mine(someHash, someData, () => false).isDefined)
    }

    "A Blockchain (that is empty)" should "have an empty last hash" in {
        assert(List.empty[Block].lastHash.isEmpty)
    }

    it should "be valid" in {
        assert(List.empty[Block].isValid)
    }

    "A Blockchain (with one element (with empty prevHash and valid hash))" should "be valid" in {
        assert(List(someBlockWithEmptyPreviousHashAndValidHash).isValid)
    }

    "A Blockchain (with one element (with empty prevHash and invalid hash))" should "not be valid" in {
        assert(!List(someBlockWithEmptyPreviousHashAndInvalidHash).isValid)
    }

    "A Blockchain (with one element (with non-empty prevHash and valid hash))" should "not be valid" in {
        assert(!List(someBlockWithNonEmptyPreviousHashAndValidHash).isValid)
    }

    "A Blockchain (with two elements (with empty prevHash and valid hash then matching prevHash and valid hash))" should "be valid" in {
        val chain = List(
            someBlockWithEmptyPreviousHashAndValidHash,
            Block(someBlockWithEmptyPreviousHashAndValidHash.hash, List(3, 4), 18541)
        )
        
        assert(chain.isValid)
    }

    "A Blockchain (with two elements (with empty prevHash and valid hash then matching prevHash and INvalid hash))" should "not be valid" in {
        val chain = List(
            someBlockWithEmptyPreviousHashAndValidHash,
            Block(someBlockWithEmptyPreviousHashAndValidHash.hash, List(3, 4), 9999) // bad modifier
        )
        
        assert(!chain.isValid)
    }

    "A Blockchain (with two elements (with empty prevHash and valid hash then non-matching prevHash and valid hash))" should "not be valid" in {
        val chain = List(
            someBlockWithEmptyPreviousHashAndValidHash,
            someBlockWithEmptyPreviousHashAndValidHash // bad prevHash
        )
        
        assert(!chain.isValid)
    }

}