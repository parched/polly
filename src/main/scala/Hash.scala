import java.security.MessageDigest

object Hash:
    def sha256(data: List[Byte]): List[Byte] =
        MessageDigest.getInstance("SHA-256").digest(data.toArray).toList
