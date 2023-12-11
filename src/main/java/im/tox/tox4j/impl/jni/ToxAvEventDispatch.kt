package im.tox.tox4j.impl.jni

import com.google.protobuf.ByteString
import im.tox.tox4j.av.callbacks.AudioBitRateCallback
import im.tox.tox4j.av.callbacks.AudioReceiveFrameCallback
import im.tox.tox4j.av.callbacks.CallCallback
import im.tox.tox4j.av.callbacks.CallStateCallback
import im.tox.tox4j.av.callbacks.ToxAvEventListener
import im.tox.tox4j.av.callbacks.VideoBitRateCallback
import im.tox.tox4j.av.callbacks.VideoReceiveFrameCallback
import im.tox.tox4j.av.data.AudioChannels
import im.tox.tox4j.av.data.BitRate
import im.tox.tox4j.av.data.Height
import im.tox.tox4j.av.data.SamplingRate
import im.tox.tox4j.av.data.Width
import im.tox.tox4j.av.proto.AudioBitRate
import im.tox.tox4j.av.proto.AudioReceiveFrame
import im.tox.tox4j.av.proto.AvEvents
import im.tox.tox4j.av.proto.Call
import im.tox.tox4j.av.proto.CallState
import im.tox.tox4j.av.proto.VideoBitRate
import im.tox.tox4j.av.proto.VideoReceiveFrame
import im.tox.tox4j.core.data.ToxFriendNumber
import java.util.EnumSet

object ToxAvEventDispatch {

    private fun <S> dispatchCall(
        handler: CallCallback<S>,
        callList: List<Call>,
        state: S,
    ): S {
        return callList.fold(state) { state, call ->
            handler.call(
                ToxFriendNumber.unsafeFromInt(call.friendNumber),
                audioEnabled = call.audioEnabled,
                videoEnabled = call.videoEnabled,
                state = state,
            )
        }
    }

    private fun <S> dispatchCallState(
        handler: CallStateCallback<S>,
        callStateList: List<CallState>,
        state: S,
    ): S {
        return callStateList.fold(state) { state, callState ->
            handler.callState(
                friendNumber = ToxFriendNumber.unsafeFromInt(callState.friendNumber),
                callState = callState.callStateList.map(ToxAvProtoConverter::convert).toEnumSet(),
                state = state,
            )
        }
    }

    private fun <S> dispatchAudioBitRate(
        handler: AudioBitRateCallback<S>,
        audioBitRateList: List<AudioBitRate>,
        state: S,
    ): S {
        return audioBitRateList.fold(state) { state, audioBitRate ->
            handler.audioBitRate(
                friendNumber = ToxFriendNumber.unsafeFromInt(audioBitRate.friendNumber),
                audioBitRate = BitRate.unsafeFromInt(audioBitRate.audioBitRate),
                state = state,
            )
        }
    }

    private fun <S> dispatchVideoBitRate(
        handler: VideoBitRateCallback<S>,
        videoBitRateList: List<VideoBitRate>,
        state: S,
    ): S {
        return videoBitRateList.fold(state) { state, videoBitRate ->
            handler.videoBitRate(
                friendNumber = ToxFriendNumber.unsafeFromInt(videoBitRate.friendNumber),
                videoBitRate = BitRate.unsafeFromInt(videoBitRate.videoBitRate),
                state = state,
            )
        }
    }

    private fun <S> dispatchAudioReceiveFrame(
        handler: AudioReceiveFrameCallback<S>,
        audioReceiveFrameList: List<AudioReceiveFrame>,
        state: S,
    ): S {
        return audioReceiveFrameList.fold(state) { state, audioReceiveFrame ->
            handler.audioReceiveFrame(
                friendNumber = ToxFriendNumber.unsafeFromInt(audioReceiveFrame.friendNumber),
                pcm = audioReceiveFrame.pcm.toShortArray(),
                channels = AudioChannels.unsafeFromInt(audioReceiveFrame.channels),
                samplingRate = SamplingRate.unsafeFromInt(audioReceiveFrame.samplingRate),
                state = state,
            )
        }
    }

    private fun <S> dispatchVideoReceiveFrame(
        handler: VideoReceiveFrameCallback<S>,
        videoReceiveFrameList: List<VideoReceiveFrame>,
        state: S,
    ): S {
        return videoReceiveFrameList.fold(state) { state, videoReceiveFrame ->
            val width = Width.unsafeFromInt(videoReceiveFrame.width)
            val height = Height.unsafeFromInt(videoReceiveFrame.height)

            val (yArray, uArray, vArray) = ToxAvProtoConverter.convert(
                arrays = handler.videFrameCachedYUV(
                    height = height,
                    yStride = videoReceiveFrame.yStride,
                    uStride = videoReceiveFrame.uStride,
                    vStride = videoReceiveFrame.vStride
                ),
                y = videoReceiveFrame.y,
                u = videoReceiveFrame.u,
                v = videoReceiveFrame.v,
            )

            handler.videoReceiveFrame(
                friendNumber = ToxFriendNumber.unsafeFromInt(videoReceiveFrame.friendNumber),
                width = width,
                height = height,
                y = yArray,
                u = uArray,
                v = vArray,
                yStride = videoReceiveFrame.yStride,
                uStride = videoReceiveFrame.uStride,
                vStride = videoReceiveFrame.vStride,
                state = state,
            )
        }
    }

    private inline fun <reified T : Enum<T>> List<T>.toEnumSet(): EnumSet<T> {
        return EnumSet.noneOf(T::class.java).also { set ->
            set.addAll(this)
        }
    }

    private fun ByteString.toShortArray(): ShortArray {
        val shortBuffer = asReadOnlyByteBuffer().asShortBuffer()
        val shortArray = ShortArray(shortBuffer.capacity())
        shortBuffer.get(shortArray)
        return shortArray
    }

    fun <S> dispatch(
        eventListener: ToxAvEventListener<S>,
        eventData: ByteArray?,
        state: S
    ): S {
        if (eventData == null) {
            return state
        }

        val events = AvEvents.parseFrom(eventData)

        return events.run {
            sequence<(S) -> S> {
                yield { dispatchCall(eventListener, callList, it) }
                yield { dispatchCallState(eventListener, callStateList, it) }
                yield { dispatchAudioBitRate(eventListener, audioBitRateList, it) }
                yield { dispatchVideoBitRate(eventListener, videoBitRateList, it) }
                yield { dispatchAudioReceiveFrame(eventListener, audioReceiveFrameList, it) }
                yield { dispatchVideoReceiveFrame(eventListener, videoReceiveFrameList, it) }
            }
        }.fold(state) { state, dispatcher ->
            dispatcher.invoke(state)
        }
    }

}
