package im.tox.tox4j.impl.jni

import java.util

import com.google.protobuf.ByteString
import com.typesafe.scalalogging.Logger
import im.tox.tox4j.OptimisedIdOps._
import im.tox.tox4j.av.callbacks._
import im.tox.tox4j.av.data._
import im.tox.tox4j.av.enums.ToxavFriendCallState
import im.tox.tox4j.av.proto._
import im.tox.tox4j.core.data.ToxFriendNumber
import org.jetbrains.annotations.Nullable
import org.slf4j.LoggerFactory

object ToxAvEventDispatch {

  private val logger = Logger(LoggerFactory.getLogger(getClass))

  private val IntBytes = Integer.SIZE / java.lang.Byte.SIZE



  private def dispatchCall[S](handler: CallCallback[S], call: Seq[Call])(state: S): S = {
    call.foldLeft(state) {
      case (state, Call(friendNumber, audioEnabled, videoEnabled)) =>
        handler.call(
          ToxFriendNumber.unsafeFromInt(friendNumber),
          audioEnabled,
          videoEnabled
        )(state)
    }
  }

  private def dispatchCallState[S](handler: CallStateCallback[S], callState: Seq[CallState])(state: S): S = {
    callState.foldLeft(state) {
      case (state, CallState(friendNumber, callStateHead +: callStateTail)) =>
        handler.callState(
          ToxFriendNumber.unsafeFromInt(friendNumber),
          util.EnumSet.of(convert(callStateHead), callStateTail.map(convert): _*)
        )(state)
    }
  }

  private def dispatchAudioBitRate[S](handler: AudioBitRateCallback[S], audioBitRate: Seq[AudioBitRate])(state: S): S = {
    audioBitRate.foldLeft(state) {
      case (state, AudioBitRate(friendNumber, audioBitRate)) =>
        handler.audioBitRate(
          ToxFriendNumber.unsafeFromInt(friendNumber),
          BitRate.unsafeFromInt(audioBitRate)
        )(state)
    }
  }

  private def dispatchVideoBitRate[S](handler: VideoBitRateCallback[S], videoBitRate: Seq[VideoBitRate])(state: S): S = {
    videoBitRate.foldLeft(state) {
      case (state, VideoBitRate(friendNumber, videoBitRate)) =>
        handler.videoBitRate(
          ToxFriendNumber.unsafeFromInt(friendNumber),
          BitRate.unsafeFromInt(videoBitRate)
        )(state)
    }
  }

  private def toShortArray(bytes: ByteString): Array[Short] = {
    val shortBuffer = bytes.asReadOnlyByteBuffer().asShortBuffer()
    val shortArray = Array.ofDim[Short](shortBuffer.capacity)
    shortBuffer.get(shortArray)
    shortArray
  }

  private def dispatchAudioReceiveFrame[S](handler: AudioReceiveFrameCallback[S], audioReceiveFrame: Seq[AudioReceiveFrame])(state: S): S = {
    audioReceiveFrame.foldLeft(state) {
      case (state, AudioReceiveFrame(friendNumber, pcm, channels, samplingRate)) =>
        handler.audioReceiveFrame(
          ToxFriendNumber.unsafeFromInt(friendNumber),
          toShortArray(pcm),
          AudioChannels.unsafeFromInt(channels),
          SamplingRate.unsafeFromInt(samplingRate)
        )(state)
    }
  }

  private def convert(
    arrays: Option[(Array[Byte], Array[Byte], Array[Byte])],
    y: ByteString, u: ByteString, v: ByteString
  ): (Array[Byte], Array[Byte], Array[Byte]) = {
    arrays match {
      case None =>
        (y.toByteArray, u.toByteArray, v.toByteArray)
      case Some(arrays) =>
        y.copyTo(arrays._1, 0)
        u.copyTo(arrays._2, 0)
        v.copyTo(arrays._3, 0)
        arrays
    }
  }

  private def dispatchVideoReceiveFrame[S](handler: VideoReceiveFrameCallback[S], videoReceiveFrame: Seq[VideoReceiveFrame])(state: S): S = {
    videoReceiveFrame.foldLeft(state) {
      case (state, VideoReceiveFrame(friendNumber, width, height, y, u, v, yStride, uStride, vStride)) =>
        val w = Width.unsafeFromInt(width)
        val h = Height.unsafeFromInt(height)
        val (yArray, uArray, vArray) = convert(handler.videoFrameCachedYUV(h, yStride, uStride, vStride), y, u, v)

        handler.videoReceiveFrame(
          ToxFriendNumber.unsafeFromInt(friendNumber),
          w,
          h,
          yArray,
          uArray,
          vArray,
          yStride,
          uStride,
          vStride
        )(state)
    }
  }

  private def dispatchEvents[S](handler: ToxAvEventListener[S], events: AvEvents)(state: S): S = {
    (state
      |> dispatchCall(handler, events.call)
      |> dispatchCallState(handler, events.callState)
      |> dispatchAudioBitRate(handler, events.audioBitRate)
      |> dispatchVideoBitRate(handler, events.videoBitRate)
      |> dispatchAudioReceiveFrame(handler, events.audioReceiveFrame)
      |> dispatchVideoReceiveFrame(handler, events.videoReceiveFrame))
  }

  private def decodeInt32(eventData: Array[Byte]): Int = {
    assert(eventData.length >= IntBytes)
    (0
      | eventData(0) << (8 * 3)
      | eventData(1) << (8 * 2)
      | eventData(2) << (8 * 1)
      | eventData(3) << (8 * 0))
  }

  @SuppressWarnings(Array(
    "org.wartremover.warts.ArrayEquals",
    "org.wartremover.warts.Equals",
    "org.wartremover.warts.Null"
  ))
  def dispatch[S](handler: ToxAvEventListener[S], @Nullable eventData: Array[Byte])(state: S): S = {
    if (eventData == null) { // scalastyle:ignore null
      state
    } else {
      val events = AvEvents.parseFrom(eventData)
      dispatchEvents(handler, events)(state)
    }
  }

}
