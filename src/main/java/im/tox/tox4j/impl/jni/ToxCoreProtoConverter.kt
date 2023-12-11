package im.tox.tox4j.impl.jni

import im.tox.tox4j.core.enums.*
import im.tox.tox4j.core.proto.*

internal object ToxCoreProtoConverter {

    fun convert(status: Connection.Type): ToxConnection {
        return when (status) {
            Connection.Type.NONE -> ToxConnection.NONE
            Connection.Type.TCP -> ToxConnection.TCP
            Connection.Type.UDP -> ToxConnection.UDP
            Connection.Type.UNRECOGNIZED ->
                throw IllegalArgumentException("Unrecognized connection type")
        }
    }

    fun convert(status: UserStatus.Type): ToxUserStatus {
        return when (status) {
            UserStatus.Type.NONE -> ToxUserStatus.NONE
            UserStatus.Type.AWAY -> ToxUserStatus.AWAY
            UserStatus.Type.BUSY -> ToxUserStatus.BUSY
            UserStatus.Type.UNRECOGNIZED ->
                throw IllegalArgumentException("Unrecognized user status type")
        }
    }

    fun convert(status: ToxUserStatus): UserStatus.Type {
        return when (status) {
            ToxUserStatus.NONE -> UserStatus.Type.NONE
            ToxUserStatus.AWAY -> UserStatus.Type.AWAY
            ToxUserStatus.BUSY -> UserStatus.Type.BUSY
        }
    }

    fun convert(control: FileControl.Type): ToxFileControl {
        return when (control) {
            FileControl.Type.RESUME -> ToxFileControl.RESUME
            FileControl.Type.PAUSE -> ToxFileControl.PAUSE
            FileControl.Type.CANCEL -> ToxFileControl.CANCEL
            FileControl.Type.UNRECOGNIZED ->
                throw IllegalArgumentException("Unrecognized file control type")
        }
    }

    fun convert(messageType: MessageType.Type): ToxMessageType {
        return when (messageType) {
            MessageType.Type.NORMAL -> ToxMessageType.NORMAL
            MessageType.Type.ACTION -> ToxMessageType.ACTION
            MessageType.Type.UNRECOGNIZED ->
                throw IllegalArgumentException("Unrecognized message type")
        }
    }

    fun convert(fileKind: Int): Int {
        return fileKind
//        return when(fileKind) {
//            FileKind.Type.DATA -> ToxFileKind.DATA
//            FileKind.Type.AVATAR -> ToxFileKind.AVATAR
//            FileKind.Type.UNRECOGNIZED ->
//                throw IllegalArgumentException("Unrecognized file kind")
//        }
    }


}