package im.tox.tox4j.impl.jni

import im.tox.tox4j.crypto.ToxCrypto
import im.tox.tox4j.crypto.ToxCryptoConstants

object ToxCryptoImpl : ToxCrypto<ByteArray> {

  override fun passKeyEquals(a: ByteArray, b: ByteArray): Boolean {
    return a.contentEquals(b)
  }

  override fun passKeyToBytes(passKey: ByteArray): List<Byte> {
    return passKey.toList()
  }

  override fun passKeyFromBytes(bytes: ByteArray): ByteArray? {
    return if (bytes.size == ToxCryptoConstants.keyLength + ToxCryptoConstants.saltLength) {
      bytes.copyOf()
    } else {
      null
    }
  }

  override fun encrypt(data: ByteArray, passKey: ByteArray): ByteArray {
    return ToxCryptoJni.toxPassKeyEncrypt(data, passKey)
  }

  override fun getSalt(data: ByteArray): ByteArray {
    return ToxCryptoJni.toxGetSalt(data)
  }

  override fun isDataEncrypted(data: ByteArray): Boolean {
    return ToxCryptoJni.toxIsDataEncrypted(data)
  }

  override fun passKeyDeriveWithSalt(passPhrase: ByteArray, salt: ByteArray): ByteArray {
    return ToxCryptoJni.toxPassKeyDeriveWithSalt(passPhrase, salt)
  }

  override fun passKeyDerive(passPhrase: ByteArray): ByteArray {
    return ToxCryptoJni.toxPassKeyDerive(passPhrase)
  }

  override fun decrypt(data: ByteArray, passKey: ByteArray): ByteArray {
    return ToxCryptoJni.toxPassKeyDecrypt(data, passKey)
  }


  override fun hash(data: ByteArray): ByteArray {
    return ToxCryptoJni.toxHash(data)
  }

}
