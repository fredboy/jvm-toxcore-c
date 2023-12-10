package im.tox.tox4j.impl.jni

import im.tox.tox4j.core.callbacks.*
import im.tox.tox4j.core.data.*
import im.tox.tox4j.core.proto.*

object ToxCoreEventDispatch {

  private fun <S> dispatchSelfConnectionStatus(
          callback: SelfConnectionStatusCallback<S>,
          selfConnectionStatusList: List<SelfConnectionStatus>,
          state: S,
  ): S {
    return selfConnectionStatusList.fold(state) { state, selfConnectionStatus ->
      callback.selfConnectionStatus(
              connectionStatus = ToxCoreProtoConverter.convert(selfConnectionStatus.connectionStatus),
              state = state,
      )
    }
  }

  private fun <S> dispatchFriendName(
          callback: FriendNameCallback<S>,
          friendNameList: List<FriendName>,
          state: S,
  ): S {
    return friendNameList.fold(state) { state, friendName ->
      callback.friendName(
              friendNumber = ToxFriendNumber.unsafeFromInt(friendName.friendNumber),
              name = ToxNickname.unsafeFromValue(friendName.name.toByteArray()),
              state = state,
      )
    }
  }

  private fun <S> dispatchFriendStatusMessage(
          callback: FriendStatusMessageCallback<S>,
          friendStatusMessageList: List<FriendStatusMessage>,
          state: S,
  ): S {
    return friendStatusMessageList.fold(state) { state, friendStatusMessage ->
      callback.friendStatusMessage(
              friendNumber = ToxFriendNumber.unsafeFromInt(friendStatusMessage.friendNumber),
              message = ToxStatusMessage.unsafeFromValue(friendStatusMessage.message.toByteArray()),
              state = state,
      )
    }
  }

  private fun <S> dispatchFriendStatus(
          callback: FriendStatusCallback<S>,
          friendStatusList: List<FriendStatus>,
          state: S,
  ): S {
    return friendStatusList.fold(state) { state, friendStatus ->
      callback.friendStatus(
              friendNumber = ToxFriendNumber.unsafeFromInt(friendStatus.friendNumber),
              status = ToxCoreProtoConverter.convert(friendStatus.status),
              state = state,
      )
    }
  }

  private fun <S> dispatchFriendConnectionStatus(
          callback: FriendConnectionStatusCallback<S>,
          friendConnectionStatusList: List<FriendConnectionStatus>,
          state: S,
  ): S {
    return friendConnectionStatusList.fold(state) { state, friendConnectionStatus ->
      callback.friendConnectionStatus(
              friendNumber = ToxFriendNumber.unsafeFromInt(friendConnectionStatus.friendNumber),
              connectionStatus = ToxCoreProtoConverter.convert(friendConnectionStatus.connectionStatus),
              state = state,
      )
    }
  }

  private fun <S> dispatchFriendTyping(
          callback: FriendTypingCallback<S>,
          friendTypingList: List<FriendTyping>,
          state: S,
  ): S {
    return friendTypingList.fold(state) { state, friendTyping ->
      callback.friendTyping(
              friendNumber = ToxFriendNumber.unsafeFromInt(friendTyping.friendNumber),
              isTyping = friendTyping.isTyping,
              state = state,
      )
    }
  }

  private fun <S> dispatchFriendReadReceipt(
          callback: FriendReadReceiptCallback<S>,
          friendReadReceiptList: List<FriendReadReceipt>,
          state: S,
  ): S {
    return friendReadReceiptList.fold(state) { state, friendReadReceipt ->
      callback.friendReadReceipt(
              friendNumber = ToxFriendNumber.unsafeFromInt(friendReadReceipt.friendNumber),
              messageId = friendReadReceipt.messageId,
              state = state,
      )
    }
  }

  private fun <S> dispatchFriendRequest(
          callback: FriendRequestCallback<S>,
          friendRequestList: List<FriendRequest>,
          state: S,
  ): S {
    return friendRequestList.fold(state) { state, friendRequest ->
      callback.friendRequest(
              publicKey = ToxPublicKey.unsafeFromValue(friendRequest.publicKey.toByteArray()),
              timeDelta = friendRequest.timeDelta,
              message = ToxFriendRequestMessage.unsafeFromValue(friendRequest.message.toByteArray()),
              state = state,
      )
    }
  }

  private fun <S> dispatchFriendMessage(
          callback: FriendMessageCallback<S>,
          friendMessageList: List<FriendMessage>,
          state: S,
  ): S {
    return friendMessageList.fold(state) { state, friendMessage ->
      callback.friendMessage(
              friendNumber = ToxFriendNumber.unsafeFromInt(friendMessage.friendNumber),
              messageType = ToxCoreProtoConverter.convert(friendMessage.type),
              timeDelta = friendMessage.timeDelta,
              message = ToxFriendMessage.unsafeFromValue(friendMessage.message.toByteArray()),
              state = state,
      )
    }
  }

  private fun <S> dispatchFileRecvControl(
          callback: FileRecvControlCallback<S>,
          fileRecvControlList: List<FileRecvControl>,
          state: S,
  ): S {
    return fileRecvControlList.fold(state) { state, fileRecvControl ->
      callback.fileRecvControl(
              friendNumber = ToxFriendNumber.unsafeFromInt(fileRecvControl.friendNumber),
              fileNumber = fileRecvControl.fileNumber,
              control = ToxCoreProtoConverter.convert(fileRecvControl.control),
              state = state,
      )
    }
  }

  private fun <S> dispatchFileChunkRequest(
          callback: FileChunkRequestCallback<S>,
          fileChunkRequestList: List<FileChunkRequest>,
          state: S,
  ): S {
    return fileChunkRequestList.fold(state) { state, fileChunkRequest ->
      callback.fileChunkRequest(
              friendNumber = ToxFriendNumber.unsafeFromInt(fileChunkRequest.friendNumber),
              fileNumber = fileChunkRequest.fileNumber,
              position = fileChunkRequest.position,
              length = fileChunkRequest.length,
              state = state,
      )
    }
  }

  private fun <S> dispatchFileRecv(
          callback: FileRecvCallback<S>,
          fileRectList: List<FileRecv>,
          state: S,
  ): S {
    return fileRectList.fold(state) { state, fileRecv ->
      callback.fileRecv(
              friendNumber = ToxFriendNumber.unsafeFromInt(fileRecv.friendNumber),
              fileNumber = fileRecv.fileNumber,
              kind = ToxCoreProtoConverter.convert(fileRecv.kind),
              fileSize = fileRecv.fileSize,
              filename = ToxFileName.unsafeFromValue(fileRecv.filename.toByteArray()),
              state = state,
      )
    }
  }

  private fun <S> dispatchFileRecvChunk(
          callback: FileRecvChunkCallback<S>,
          fileRecvChunkList: List<FileRecvChunk>,
          state: S,
  ): S {
    return fileRecvChunkList.fold(state) { state, fileRecvChunk ->
      callback.fileRecvChunk(
              friendNumber = ToxFriendNumber.unsafeFromInt(fileRecvChunk.fileNumber),
              fileNumber = fileRecvChunk.fileNumber,
              position = fileRecvChunk.position,
              data = fileRecvChunk.data.toByteArray(),
              state = state,
      )
    }
  }

  private fun <S> dispatchFriendLossyPacket(
          callback: FriendLossyPacketCallback<S>,
          friendLossyPacketList: List<FriendLossyPacket>,
          state: S,
  ): S {
    return friendLossyPacketList.fold(state) { state, friendLossyPacket ->
      callback.friendLossyPacket(
              friendNumber = ToxFriendNumber.unsafeFromInt(friendLossyPacket.friendNumber),
              data = ToxLossyPacket.unsafeFromValue(friendLossyPacket.data.toByteArray()),
              state = state,
      )
    }
  }

  private fun <S> dispatchFriendLosslessPacket(
          callback: FriendLosslessPacketCallback<S>,
          friendLosslessPacketList: List<FriendLosslessPacket>,
          state: S,
  ): S {
    return friendLosslessPacketList.fold(state) { state, friendLosslessPacket ->
      callback.friendLosslessPacket(
              friendNumber = ToxFriendNumber.unsafeFromInt(friendLosslessPacket.friendNumber),
              data = ToxLosslessPacket.unsafeFromValue(friendLosslessPacket.data.toByteArray()),
              state = state,
      )
    }
  }

  fun <S> dispatch(
          eventListener: ToxCoreEventListener<S>,
          eventData: ByteArray?,
          state: S,
  ): S {
    if (eventData == null) {
      return state
    }

    val events = CoreEvents.parseFrom(eventData)

    return events.run {
      sequence<(S) -> S> {
        yield { dispatchSelfConnectionStatus(eventListener, selfConnectionStatusList, it) }
        yield { dispatchFriendName(eventListener, friendNameList, it) }
        yield { dispatchFriendStatusMessage(eventListener, friendStatusMessageList, it) }
        yield { dispatchFriendStatus(eventListener, friendStatusList, it) }
        yield { dispatchFriendConnectionStatus(eventListener, friendConnectionStatusList, it) }
        yield { dispatchFriendTyping(eventListener, friendTypingList, it) }
        yield { dispatchFriendReadReceipt(eventListener, friendReadReceiptList, it) }
        yield { dispatchFriendRequest(eventListener, friendRequestList, it) }
        yield { dispatchFriendMessage(eventListener, friendMessageList, it) }
        yield { dispatchFileRecvControl(eventListener, fileRecvControlList, it) }
        yield { dispatchFileChunkRequest(eventListener, fileChunkRequestList, it) }
        yield { dispatchFileRecv(eventListener, fileRecvList, it) }
        yield { dispatchFileRecvChunk(eventListener, fileRecvChunkList, it) }
        yield { dispatchFriendLossyPacket(eventListener, friendLossyPacketList, it) }
        yield { dispatchFriendLosslessPacket(eventListener, friendLosslessPacketList, it) }
      }.fold(state) { state, dispatcher ->
        dispatcher.invoke(state)
      }
    }
  }

}