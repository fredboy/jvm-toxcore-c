package im.tox.tox4j.impl.jni

import im.tox.core.network.Port
import im.tox.tox4j.core.ToxCore
import im.tox.tox4j.core.ToxCoreConstants
import im.tox.tox4j.core.callbacks.ToxCoreEventListener
import im.tox.tox4j.core.data.ToxFileId
import im.tox.tox4j.core.data.ToxFileName
import im.tox.tox4j.core.data.ToxFriendAddress
import im.tox.tox4j.core.data.ToxFriendMessage
import im.tox.tox4j.core.data.ToxFriendNumber
import im.tox.tox4j.core.data.ToxFriendRequestMessage
import im.tox.tox4j.core.data.ToxLosslessPacket
import im.tox.tox4j.core.data.ToxLossyPacket
import im.tox.tox4j.core.data.ToxNickname
import im.tox.tox4j.core.data.ToxPublicKey
import im.tox.tox4j.core.data.ToxSecretKey
import im.tox.tox4j.core.data.ToxStatusMessage
import im.tox.tox4j.core.enums.ToxConnection
import im.tox.tox4j.core.enums.ToxFileControl
import im.tox.tox4j.core.enums.ToxMessageType
import im.tox.tox4j.core.enums.ToxUserStatus
import im.tox.tox4j.core.exceptions.ToxBootstrapException
import im.tox.tox4j.core.exceptions.ToxFileControlException
import im.tox.tox4j.core.exceptions.ToxFileGetException
import im.tox.tox4j.core.exceptions.ToxFileSeekException
import im.tox.tox4j.core.exceptions.ToxFileSendChunkException
import im.tox.tox4j.core.exceptions.ToxFileSendException
import im.tox.tox4j.core.exceptions.ToxFriendAddException
import im.tox.tox4j.core.exceptions.ToxFriendByPublicKeyException
import im.tox.tox4j.core.exceptions.ToxFriendCustomPacketException
import im.tox.tox4j.core.exceptions.ToxFriendDeleteException
import im.tox.tox4j.core.exceptions.ToxFriendGetPublicKeyException
import im.tox.tox4j.core.exceptions.ToxFriendSendMessageException
import im.tox.tox4j.core.exceptions.ToxGetPortException
import im.tox.tox4j.core.exceptions.ToxNewException
import im.tox.tox4j.core.exceptions.ToxSetInfoException
import im.tox.tox4j.core.exceptions.ToxSetTypingException
import im.tox.tox4j.core.options.ToxOptions
import im.tox.tox4j.impl.jni.internal.Event
import org.slf4j.LoggerFactory
import kotlin.jvm.Throws

/**
 * Initialises the new Tox instance with an optional save-data received from [[getSavedata]].
 *
 * @param options Connection options object with optional save-data.
 */
class ToxCoreImpl @Throws(ToxNewException::class) constructor(
  val options: ToxOptions
) : ToxCore {

  private val onCloseCallbacks = Event()

  /**
   * This field has package visibility for [[ToxAvImpl]].
   */
  internal val instanceNumber = ToxCoreJni.toxNew(
    options.ipv6Enabled,
    options.udpEnabled,
    options.localDiscoveryEnabled,
    options.proxyOptions.proxyType.ordinal,
    options.proxyOptions.proxyAddress,
    options.proxyOptions.proxyPort.value,
    options.startPort.value,
    options.endPort.value,
    options.tcpPort.value,
    options.saveData.kind.ordinal,
    options.saveData.data,
  )

  internal fun addOnCloseCallback(callback: () -> Unit): Event.Id = onCloseCallbacks add callback

  internal fun removeOnCloseCallback(id: Event.Id) = onCloseCallbacks remove id

  override fun load(options: ToxOptions): ToxCoreImpl = ToxCoreImpl(options)

  override fun close() {
    onCloseCallbacks()
    ToxCoreJni.toxKill(instanceNumber)
  }

  protected fun finalize() {
    try {
      close()
      ToxCoreJni.toxFinalize(instanceNumber)
    } catch (e: Throwable) {
      logger.error(
        "Exception caught in finalizer; this indicates a serious problem in native code",
        e
      )
    }
  }

  @Throws(ToxBootstrapException::class)
  override fun bootstrap(address: String, port: Port, publicKey: ToxPublicKey) {
    checkBootstrapArguments(port.value, publicKey.value)
    ToxCoreJni.toxBootstrap(instanceNumber, address, port.value, publicKey.value)
  }

  @Throws(ToxBootstrapException::class)
  override fun addTcpRelay(address: String, port: Port, publicKey: ToxPublicKey) {
    checkBootstrapArguments(port.value, publicKey.value)
    ToxCoreJni.toxAddTcpRelay(instanceNumber, address, port.value, publicKey.value)
  }

  override val saveData: ByteArray
    get() = ToxCoreJni.toxGetSavedata(instanceNumber)

  override val udpPort: Port
    @Throws(ToxGetPortException::class)
    get() = Port.unsafeFromInt(ToxCoreJni.toxSelfGetUdpPort(instanceNumber))

  override val tcpPort: Port
    @Throws(ToxGetPortException::class)
    get() = Port.unsafeFromInt(ToxCoreJni.toxSelfGetTcpPort(instanceNumber))

  override val dhtId: ToxPublicKey
    get() = ToxPublicKey.unsafeFromValue(ToxCoreJni.toxSelfGetDhtId(instanceNumber))

  override val iterationInterval: Int
    get() = ToxCoreJni.toxIterationInterval(instanceNumber)

  override fun <S> iterate(toxCoreEventListener: ToxCoreEventListener<S>, state: S): S {
    return ToxCoreEventDispatch.dispatch(
      eventListener = toxCoreEventListener,
      eventData = ToxCoreJni.toxIterate(instanceNumber),
      state = state
    )
  }

  override val publicKey: ToxPublicKey
    get() = ToxPublicKey.unsafeFromValue(ToxCoreJni.toxSelfGetPublicKey(instanceNumber))

  override val secretKey: ToxSecretKey
    get() = ToxSecretKey.unsafeFromValue(ToxCoreJni.toxSelfGetSecretKey(instanceNumber))

  override var nospam: Int
    get() = ToxCoreJni.toxSelfGetNospam(instanceNumber)
    set(value) {
      ToxCoreJni.toxSelfSetNospam(instanceNumber, value)
    }

  override val address: ToxFriendAddress
    get() = ToxFriendAddress.unsafeFromValue(ToxCoreJni.toxSelfGetAddress(instanceNumber))

  override var name: ToxNickname
    get() = ToxNickname.unsafeFromValue(ToxCoreJni.toxSelfGetName(instanceNumber))
    @Throws(ToxSetInfoException::class) set(value) {
      checkInfoNotNull(value.value)
      ToxCoreJni.toxSelfSetName(instanceNumber, value.value)
    }

  override var statusMessage: ToxStatusMessage
    get() = ToxStatusMessage.unsafeFromValue(ToxCoreJni.toxSelfGetStatusMessage(instanceNumber))
    @Throws(ToxSetInfoException::class) set(value) {
      checkInfoNotNull(value.value)
      ToxCoreJni.toxSelfSetStatusMessage(instanceNumber, value.value)
    }

  override var status: ToxUserStatus
    get() = ToxUserStatus.entries[ToxCoreJni.toxSelfGetStatus(instanceNumber)]
    set(value) {
      ToxCoreJni.toxSelfSetStatus(instanceNumber, value.ordinal)
    }

  @Throws(ToxFriendAddException::class)
  override fun addFriend(
    address: ToxFriendAddress,
    message: ToxFriendRequestMessage
  ): ToxFriendNumber {
    checkLength("Friend address", address.value, ToxCoreConstants.addressSize)
    return ToxFriendNumber.unsafeFromInt(
      ToxCoreJni.toxFriendAdd(
        instanceNumber,
        address.value,
        message.value
      )
    )
  }

  @Throws(ToxFriendAddException::class)
  override fun addFriendNoRequest(publicKey: ToxPublicKey): ToxFriendNumber {
    checkLength("Public key", publicKey.value, ToxCoreConstants.publicKeySize)
    return ToxFriendNumber.unsafeFromInt(
      ToxCoreJni.toxFriendAddNorequest(
        instanceNumber,
        publicKey.value
      )
    )
  }

  @Throws(ToxFriendDeleteException::class)
  override fun deleteFriend(friendNumber: ToxFriendNumber) {
    ToxCoreJni.toxFriendDelete(instanceNumber, friendNumber.value)
  }

  @Throws(ToxFriendByPublicKeyException::class)
  override fun friendByPublicKey(publicKey: ToxPublicKey): ToxFriendNumber {
    return ToxFriendNumber.unsafeFromInt(
      ToxCoreJni.toxFriendByPublicKey(
        instanceNumber,
        publicKey.value
      )
    )
  }

  @Throws(ToxFriendGetPublicKeyException::class)
  override fun getFriendPublicKey(friendNumber: ToxFriendNumber): ToxPublicKey {
    return ToxPublicKey.unsafeFromValue(
      ToxCoreJni.toxFriendGetPublicKey(
        instanceNumber,
        friendNumber.value
      )
    )
  }

  override fun friendExists(friendNumber: ToxFriendNumber): Boolean {
    return ToxCoreJni.toxFriendExists(instanceNumber, friendNumber.value)
  }

  override val friendList: IntArray
    get() = ToxCoreJni.toxSelfGetFriendList(instanceNumber)

  @Throws(ToxSetTypingException::class)
  override fun setTyping(friendNumber: ToxFriendNumber, typing: Boolean) {
    ToxCoreJni.toxSelfSetTyping(instanceNumber, friendNumber.value, typing)
  }

  @Throws(ToxFriendSendMessageException::class)
  override fun friendSendMessage(
    friendNumber: ToxFriendNumber,
    messageType: ToxMessageType,
    timeDelta: Int,
    message: ToxFriendMessage
  ): Int {
    return ToxCoreJni.toxFriendSendMessage(
      instanceNumber,
      friendNumber.value,
      messageType.ordinal,
      timeDelta,
      message.value
    )
  }

  @Throws(ToxFileControlException::class)
  override fun fileControl(
    friendNumber: ToxFriendNumber,
    fileNumber: Int,
    control: ToxFileControl
  ) {
    ToxCoreJni.toxFileControl(instanceNumber, friendNumber.value, fileNumber, control.ordinal)
  }

  @Throws(ToxFileSeekException::class)
  override fun fileSeek(friendNumber: ToxFriendNumber, fileNumber: Int, position: Long) {
    ToxCoreJni.toxFileSeek(instanceNumber, friendNumber.value, fileNumber, position)
  }

  @Throws(ToxFileSendException::class)
  override fun fileSend(
    friendNumber: ToxFriendNumber,
    kind: Int,
    fileSize: Long,
    fileId: ToxFileId,
    fileName: ToxFileName
  ): Int {
    return ToxCoreJni.toxFileSend(
      instanceNumber,
      friendNumber.value,
      kind,
      fileSize,
      fileId.value,
      fileName.value
    )
  }

  @Throws(ToxFileSendChunkException::class)
  override fun fileSendChunk(
    friendNumber: ToxFriendNumber,
    fileNumber: Int,
    position: Long,
    data: ByteArray
  ) {
    ToxCoreJni.toxFileSendChunk(instanceNumber, friendNumber.value, fileNumber, position, data)
  }

  @Throws(ToxFileGetException::class)
  override fun getFileFileId(friendNumber: ToxFriendNumber, fileNumber: Int): ToxFileId {
    return ToxFileId.unsafeFromValue(
      ToxCoreJni.toxFileGetFileId(
        instanceNumber,
        friendNumber.value,
        fileNumber
      )
    )
  }

  @Throws(ToxFriendCustomPacketException::class)
  override fun friendSendLossyPacket(friendNumber: ToxFriendNumber, data: ToxLossyPacket) {
    ToxCoreJni.toxFriendSendLossyPacket(instanceNumber, friendNumber.value, data.value)
  }

  @Throws(ToxFriendCustomPacketException::class)
  override fun friendSendLosslessPacket(friendNumber: ToxFriendNumber, data: ToxLosslessPacket) {
    ToxCoreJni.toxFriendSendLosslessPacket(instanceNumber, friendNumber.value, data.value)
  }

  fun invokeFriendName(friendNumber: ToxFriendNumber, name: ToxNickname) =
    ToxCoreJni.invokeFriendName(instanceNumber, friendNumber.value, name.value)

  fun invokeFriendStatusMessage(friendNumber: ToxFriendNumber, message: ByteArray) =
    ToxCoreJni.invokeFriendStatusMessage(instanceNumber, friendNumber.value, message)

  fun invokeFriendStatus(friendNumber: ToxFriendNumber, status: ToxUserStatus) =
    ToxCoreJni.invokeFriendStatus(instanceNumber, friendNumber.value, status.ordinal)

  fun invokeFriendConnectionStatus(
    friendNumber: ToxFriendNumber,
    connectionStatus: ToxConnection
  ) = ToxCoreJni.invokeFriendConnectionStatus(
    instanceNumber,
    friendNumber.value,
    connectionStatus.ordinal
  )

  fun invokeFriendTyping(friendNumber: ToxFriendNumber, isTyping: Boolean) =
    ToxCoreJni.invokeFriendTyping(instanceNumber, friendNumber.value, isTyping)

  fun invokeFriendReadReceipt(friendNumber: ToxFriendNumber, messageId: Int) =
    ToxCoreJni.invokeFriendReadReceipt(instanceNumber, friendNumber.value, messageId)

  fun invokeFriendRequest(publicKey: ToxPublicKey, timeDelta: Int, message: ByteArray) =
    ToxCoreJni.invokeFriendRequest(instanceNumber, publicKey.value, timeDelta, message)

  fun invokeFriendMessage(
    friendNumber: ToxFriendNumber,
    messageType: ToxMessageType,
    timeDelta: Int,
    message: ByteArray
  ) = ToxCoreJni.invokeFriendMessage(
    instanceNumber,
    friendNumber.value,
    messageType.ordinal,
    timeDelta,
    message
  )

  fun invokeFileChunkRequest(
    friendNumber: ToxFriendNumber,
    fileNumber: Int,
    position: Long,
    length: Int
  ) = ToxCoreJni.invokeFileChunkRequest(
    instanceNumber,
    friendNumber.value,
    fileNumber,
    position,
    length
  )

  fun invokeFileRecv(
    friendNumber: ToxFriendNumber,
    fileNumber: Int,
    kind: Int,
    fileSize: Long,
    filename: ByteArray
  ) = ToxCoreJni.invokeFileRecv(
    instanceNumber,
    friendNumber.value,
    fileNumber,
    kind,
    fileSize,
    filename
  )

  fun invokeFileRecvChunk(
    friendNumber: ToxFriendNumber,
    fileNumber: Int,
    position: Long,
    data: ByteArray
  ) = ToxCoreJni.invokeFileRecvChunk(
    instanceNumber,
    friendNumber.value,
    fileNumber,
    position,
    data
  )

  fun invokeFileRecvControl(
    friendNumber: ToxFriendNumber,
    fileNumber: Int,
    control: ToxFileControl
  ) = ToxCoreJni.invokeFileRecvControl(
    instanceNumber,
    friendNumber.value,
    fileNumber,
    control.ordinal
  )

  fun invokeFriendLossyPacket(friendNumber: ToxFriendNumber, data: ByteArray) =
    ToxCoreJni.invokeFriendLossyPacket(instanceNumber, friendNumber.value, data)

  fun invokeFriendLosslessPacket(friendNumber: ToxFriendNumber, data: ByteArray) =
    ToxCoreJni.invokeFriendLosslessPacket(instanceNumber, friendNumber.value, data)

  fun invokeSelfConnectionStatus(connectionStatus: ToxConnection) =
    ToxCoreJni.invokeSelfConnectionStatus(instanceNumber, connectionStatus.ordinal)


  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Throws(ToxBootstrapException::class)
    private fun checkBootstrapArguments(port: Int, publicKey: ByteArray?) {
      if (port < 0) {
        throw ToxBootstrapException(
          ToxBootstrapException.Code.BAD_PORT,
          "Port cannot be negative"
        )
      }

      if (port > 65535) {
        throw ToxBootstrapException(
          ToxBootstrapException.Code.BAD_PORT,
          "Port cannot exceed 65535"
        )
      }

      if (publicKey != null) {
        if (publicKey.size < ToxCoreConstants.publicKeySize) {
          throw ToxBootstrapException(ToxBootstrapException.Code.BAD_KEY, "Key too short")
        }
        if (publicKey.size > ToxCoreConstants.publicKeySize) {
          throw ToxBootstrapException(ToxBootstrapException.Code.BAD_KEY, "Key too long")
        }
      }
    }

    private fun throwLengthException(name: String, message: String, expectedSize: Int) {
      throw IllegalArgumentException("$name too $message, must be $expectedSize bytes")
    }

    private fun checkLength(name: String, bytes: ByteArray?, expectedSize: Int) {
      if (bytes != null) {
        if (bytes.size < expectedSize) {
          throwLengthException(name, "sjort", expectedSize)
        }

        if (bytes.size > expectedSize) {
          throwLengthException(name, "long", expectedSize)
        }
      }
    }

    @Throws(ToxSetInfoException::class)
    private fun checkInfoNotNull(info: ByteArray?) {
      if (info == null) {
        throw ToxSetInfoException(ToxSetInfoException.Code.NULL)
      }
    }
  }
}
