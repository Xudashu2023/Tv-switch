package com.tvswitch.app.adb

import android.content.Context
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

/**
 * Tv Switch 专用的 ADB 密钥管理（原创实现）。
 * 密钥只存在应用私有目录，不上传、不外发。
 *
 * ADB 公钥格式说明（公开协议）：
 * 公钥字符串 = Base64( 小端 uint32: 模数以 32 位字为单位的长度
 *   + 模数按小端字序排列的字节 + 小端 uint32: 指数 ) + " TvSwitch@android"
 * 签名使用 SHA256withRSA，对电视发来的 20 字节随机令牌签名。
 * 该格式与实现均按公开 ADB 协议资料编写，未复制任何第三方源码。
 */
class AdbKeyStore(context: Context) {
    private val dir: File = context.filesDir
    private val privFile = File(dir, "adb_private_key")
    private val pubFile = File(dir, "adb_public_key")

    data class Keys(val privateKey: PrivateKey, val publicKeyString: String)

    fun loadOrCreate(): Keys {
        if (privFile.exists() && pubFile.exists()) {
            return try {
                val privBytes = Base64.getDecoder().decode(privFile.readText().trim())
                val priv = KeyFactory.getInstance("RSA")
                    .generatePrivate(PKCS8EncodedKeySpec(privBytes))
                Keys(priv, pubFile.readText().trim())
            } catch (e: Exception) {
                create()
            }
        }
        return create()
    }

    private fun create(): Keys {
        val gen = KeyPairGenerator.getInstance("RSA")
        gen.initialize(2048)
        val pair = gen.generateKeyPair()
        val privB64 = Base64.getEncoder().encodeToString(pair.private.encoded)
        privFile.writeText(privB64)
        val pub = encodePublicKey(pair.public.encoded)
        pubFile.writeText(pub)
        return Keys(pair.private, pub)
    }

    /** 从标准 X.509 公钥字节中取出模数与指数，再按 ADB 格式重编码。 */
    private fun encodePublicKey(x509: ByteArray): String {
        // 解析 X.509 SubjectPublicKeyInfo：最后两个 INTEGER 分别是模数与指数。
        // 为避免引入 ASN.1 解析库，这里用 Java 自带的 KeyFactory 反解析。
        val kf = KeyFactory.getInstance("RSA")
        val pub = kf.generatePublic(java.security.spec.X509EncodedKeySpec(x509))
                as java.security.interfaces.RSAPublicKey
        val modulus = pub.modulus.toByteArray()
        // 去掉 BigInteger 可能补的符号位 0x00
        val mod = if (modulus.isNotEmpty() && modulus[0] == 0.toByte()) {
            modulus.copyOfRange(1, modulus.size)
        } else modulus
        // 模数按小端字序输出：先把大端字节反转，再按 4 字节一组补齐
        val words = (mod.size + 3) / 4
        val buf = ByteBuffer.allocate(4 + words * 4 + 4).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(words)
        val reversed = mod.reversedArray()
        buf.put(reversed)
        repeat(words * 4 - reversed.size) { buf.put(0) }
        buf.putInt(pub.publicExponent.toInt())
        val b64 = Base64.getEncoder().encodeToString(buf.array())
        return "$b64 TvSwitch@android"
    }

    fun sign(privateKey: PrivateKey, token: ByteArray): ByteArray {
        val sig = Signature.getInstance("SHA256withRSA")
        sig.initSign(privateKey)
        sig.update(token)
        return sig.sign()
    }

    /** 电视上“允许调试”弹窗里显示的指纹：公钥字符串的 SHA-256 前 8 组十六进制（仅用于核对）。 */
    fun fingerprint(publicKeyString: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(publicKeyString.toByteArray(Charsets.UTF_8))
        return digest.take(16).joinToString(":") { "%02X".format(it) }
    }
}
