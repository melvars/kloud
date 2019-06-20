package space.anity

import java.io.*
import java.security.*
import javax.crypto.*
import javax.crypto.spec.*

class CryptoHandler @Throws(NoSuchPaddingException::class, NoSuchAlgorithmException::class)
internal constructor(private val secretKey: SecretKey, cipher: String) {
    private val cipher: Cipher = Cipher.getInstance(cipher)

    @Throws(InvalidKeyException::class, IOException::class)
    internal fun encrypt(content: String, fileName: String) {
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv

        FileOutputStream(fileName).use { fileOut ->
            fileOut.write(iv)
            CipherOutputStream(fileOut, cipher).use { cipherOut ->
                cipherOut.write(content.toByteArray())
            }
        }
    }

    @Throws(InvalidAlgorithmParameterException::class, InvalidKeyException::class, IOException::class)
    internal fun decrypt(fileName: String): String {
        var content = ""

        FileInputStream(fileName).use { fileIn ->
            val iv = ByteArray(16)
            fileIn.read(iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))

            CipherInputStream(fileIn, cipher).use { cipherIn ->
                InputStreamReader(cipherIn).use { inputReader ->
                    BufferedReader(inputReader).use { reader ->
                        val sb = StringBuilder()
                        var line: String? = reader.readLine()
                        while (line != null) {
                            sb.append(line)
                            line = reader.readLine()
                        }
                        content = sb.toString()
                    }
                }
            }
        }

        return content // TODO: Fix char handling as 1 byte in decryption
    }
}
