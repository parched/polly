@ignore
Feature:

Scenario:
    * def MessageDigest = Java.type('java.security.MessageDigest')
    * def Base64 = Java.type('java.util.Base64')
    * def JString = Java.type('java.lang.String')
    * def ByteBuffer = Java.type('java.nio.ByteBuffer')
    * def ByteOrder = Java.type('java.nio.ByteOrder')
    * def toBase64 =
    """
    function (bytes) {
        return Base64.getEncoder().encodeToString(bytes)
    }
    """
    * def fromBase64 =
    """
    function (s) {
        return Base64.getDecoder().decode(s.getBytes());
    }
    """
    * def isValidChain =
    """
    function (chain) {
        const getHash = function (block) {
            const hasher = MessageDigest.getInstance('SHA-256');
            hasher.update(fromBase64(block.prev_hash));
            hasher.update(fromBase64(block.data));
            hasher.update(ByteBuffer.allocate(4)
                                    .order(ByteOrder.LITTLE_ENDIAN)
                                    .putInt(block.modifier)
                                    .flip());
            return hasher.digest();
        }

        const isValidHash = function (hash) {
            return hash[31] == 0 && hash[30] == 0
        }

        if (chain.length > 0) {
            if (chain[0].prev_hash != '') {
                print('invalid first prev_hash')
                return false;
            }
            if (!isValidHash(getHash(chain[chain.length - 1]))) {
                print('invalid final hash')
                return false;
            }
        }

        let i;
        for (i = 0; i < chain.length - 1; ++i) {
            const hash = getHash(chain[i]);
            if (toBase64(hash) != chain[i + 1].prev_hash) {
                print(`invalid prev_hash at ${i + 1}:\n${chain[i + 1].prev_hash}\nexpected\n\"${toBase64(hash)}\"`)
                return false;
            }
            if (!isValidHash(hash)) {
                print(`invalid hash at $i`)
                return false;
            }
        }
        return true;
    }
    """
    * def fromBase64ToString = function (b) { return new JString(fromBase64(b));}
    * def dataOf =
    """
    function (blocks) {
        return blocks.map(b => fromBase64ToString(b.data));
    }
    """