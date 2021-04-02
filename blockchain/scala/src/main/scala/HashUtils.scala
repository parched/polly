import java.security.MessageDigest

object HashUtils:
    def sha256(data: List[Byte]): List[Byte] =
        MessageDigest.getInstance("SHA-256").digest(data.toArray).toList
