package im.tox.tox4j.impl.jni

import com.google.protobuf.ByteString
import im.tox.tox4j.av.enums.ToxavFriendCallState
import im.tox.tox4j.av.proto.CallState
import java.util.EnumSet

internal object ToxAvProtoConverter {

    fun convert(kind: CallState.Kind): ToxavFriendCallState {
        return when (kind) {
            CallState.Kind.ERROR -> ToxavFriendCallState.ERROR
            CallState.Kind.FINISHED -> ToxavFriendCallState.FINISHED
            CallState.Kind.SENDING_A -> ToxavFriendCallState.SENDING_A
            CallState.Kind.SENDING_V -> ToxavFriendCallState.SENDING_V
            CallState.Kind.ACCEPTING_A -> ToxavFriendCallState.ACCEPTING_A
            CallState.Kind.ACCEPTING_V -> ToxavFriendCallState.ACCEPTING_V
            CallState.Kind.UNRECOGNIZED ->
                throw IllegalArgumentException("Unrecognized call state kind.")
        }
    }

    fun convert(callState: EnumSet<ToxavFriendCallState>): Int {
        return callState.fold(0) { bitMask, state ->
            val nextMask = when (state) {
                ToxavFriendCallState.ERROR -> 1 shl 0
                ToxavFriendCallState.FINISHED -> 1 shl 1
                ToxavFriendCallState.SENDING_A -> 1 shl 2
                ToxavFriendCallState.SENDING_V -> 1 shl 3
                ToxavFriendCallState.ACCEPTING_A -> 1 shl 4
                ToxavFriendCallState.ACCEPTING_V -> 1 shl 5
                else -> 0
            }
            bitMask or nextMask
        }
    }

    fun convert(
        arrays: Triple<ByteArray, ByteArray, ByteArray>?,
        y: ByteString,
        u: ByteString,
        v: ByteString,
    ): Triple<ByteArray, ByteArray, ByteArray> {
        return arrays?.let {
            y.copyTo(it.first, 0)
            u.copyTo(it.second, 0)
            v.copyTo(it.third, 0)
            it
        } ?: Triple(y.toByteArray(), u.toByteArray(), v.toByteArray())
    }

}