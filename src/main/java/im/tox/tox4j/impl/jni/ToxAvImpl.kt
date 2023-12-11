package im.tox.tox4j.impl.jni

import im.tox.tox4j.av.ToxAv
import im.tox.tox4j.av.callbacks.ToxAvEventListener
import im.tox.tox4j.av.data.AudioChannels
import im.tox.tox4j.av.data.BitRate
import im.tox.tox4j.av.data.Height
import im.tox.tox4j.av.data.SampleCount
import im.tox.tox4j.av.data.SamplingRate
import im.tox.tox4j.av.data.Width
import im.tox.tox4j.av.enums.ToxavCallControl
import im.tox.tox4j.av.enums.ToxavFriendCallState
import im.tox.tox4j.av.exceptions.ToxavAnswerException
import im.tox.tox4j.av.exceptions.ToxavBitRateSetException
import im.tox.tox4j.av.exceptions.ToxavCallControlException
import im.tox.tox4j.av.exceptions.ToxavCallException
import im.tox.tox4j.av.exceptions.ToxavNewException
import im.tox.tox4j.av.exceptions.ToxavSendFrameException
import im.tox.tox4j.core.ToxCore
import im.tox.tox4j.core.data.ToxFriendNumber
import org.slf4j.LoggerFactory
import java.lang.ClassCastException
import java.util.EnumSet

class ToxAvImpl @Throws(ToxavNewException::class) constructor(
  private val tox: ToxCoreImpl
) : ToxAv {

  private val onClose = tox.addOnCloseCallback(::close)

  internal val instanceNumber = ToxAvJni.toxavNew(tox.instanceNumber)

  override fun create(tox: ToxCore): ToxAv {
    return try {
      ToxAvImpl(tox as ToxCoreImpl)
    } catch (e: ClassCastException) {
      throw ToxavNewException(
        ToxavNewException.Code.INCOMPATIBLE,
        tox::class.java.canonicalName
      )
    }
  }

  override fun close() {
    tox.removeOnCloseCallback(onClose)
    ToxAvJni.toxavKill(instanceNumber)
  }

  protected fun finalize() {
    try {
      ToxAvJni.toxavFinalize(instanceNumber)
    } catch (e: Throwable) {
      logger.error(
        "Exception caught in finalizer; this indicates a serious problem in native code",
        e
      )
    }
  }

  override fun <S> iterate(handler: ToxAvEventListener<S>, state: S): S {
    return ToxAvEventDispatch.dispatch(handler, ToxAvJni.toxavIterate(instanceNumber), state)
  }

  override val iterationInterval: Int
    get() = ToxAvJni.toxavIterationInterval(instanceNumber)

  @Throws(ToxavCallException::class)
  override fun call(friendNumber: ToxFriendNumber, audioBitRate: BitRate, videoBitRate: BitRate) {
    ToxAvJni.toxavCall(
      instanceNumber,
      friendNumber.value,
      audioBitRate.value,
      videoBitRate.value
    )
  }

  @Throws(ToxavAnswerException::class)
  override fun answer(
    friendNumber: ToxFriendNumber,
    audioBitRate: BitRate,
    videoBitRate: BitRate
  ) {
    ToxAvJni.toxavAnswer(
      instanceNumber,
      friendNumber.value,
      audioBitRate.value,
      videoBitRate.value
    )
  }

  @Throws(ToxavCallControlException::class)
  override fun callControl(friendNumber: ToxFriendNumber, control: ToxavCallControl) {
    ToxAvJni.toxavCallControl(instanceNumber, friendNumber.value, control.ordinal)
  }

  @Throws(ToxavBitRateSetException::class)
  override fun setAudioBitRate(friendNumber: ToxFriendNumber, audioBitRate: BitRate) {
    ToxAvJni.toxavAudioSetBitRate(instanceNumber, friendNumber.value, audioBitRate.value)
  }

  @Throws(ToxavBitRateSetException::class)
  override fun setVideoBitRate(friendNumber: ToxFriendNumber, videoBitRate: BitRate) {
    ToxAvJni.toxavVideoSetBitRate(instanceNumber, friendNumber.value, videoBitRate.value)
  }

  @Throws(ToxavSendFrameException::class)
  override fun audioSendFrame(
    friendNumber: ToxFriendNumber,
    pcm: ShortArray,
    sampleCount: SampleCount,
    channels: AudioChannels,
    samplingRate: SamplingRate
  ) {
    ToxAvJni.toxavAudioSendFrame(
      instanceNumber,
      friendNumber.value,
      pcm,
      sampleCount.value,
      channels.value,
      samplingRate.value
    )
  }

  @Throws(ToxavSendFrameException::class)
  override fun videoSendFrame(
    friendNumber: ToxFriendNumber,
    width: Int,
    height: Int,
    y: ByteArray,
    u: ByteArray,
    v: ByteArray
  ) {
    ToxAvJni.toxavVideoSendFrame(instanceNumber, friendNumber.value, width, height, y, u, v)
  }

  override fun invokeAudioReceiveFrame(
    friendNumber: ToxFriendNumber,
    pcm: ShortArray,
    channels: AudioChannels,
    samplingRate: SamplingRate
  ) = ToxAvJni.invokeAudioReceiveFrame(
    instanceNumber,
    friendNumber.value,
    pcm,
    channels.value,
    samplingRate.value
  )

  override fun invokeAudioBitRate(friendNumber: ToxFriendNumber, audioBitRate: BitRate) =
    ToxAvJni.invokeAudioBitRate(instanceNumber, friendNumber.value, audioBitRate.value)

  override fun invokeVideoBitRate(friendNumber: ToxFriendNumber, videoBitRate: BitRate) =
    ToxAvJni.invokeVideoBitRate(instanceNumber, friendNumber.value, videoBitRate.value)

  override fun invokeCall(
    friendNumber: ToxFriendNumber,
    audioEnabled: Boolean,
    videoEnabled: Boolean
  ) = ToxAvJni.invokeCall(instanceNumber, friendNumber.value, audioEnabled, videoEnabled)

  override fun invokeCallState(
    friendNumber: ToxFriendNumber,
    callState: EnumSet<ToxavFriendCallState>
  ) = ToxAvJni.invokeCallState(
    instanceNumber,
    friendNumber.value,
    ToxAvProtoConverter.convert(callState)
  )

  override fun invokeVideoReceiveFrame(
    friendNumber: ToxFriendNumber,
    width: Width,
    height: Height,
    y: ByteArray,
    u: ByteArray,
    v: ByteArray,
    yStride: Int,
    uStride: Int,
    vStride: Int
  ) = ToxAvJni.invokeVideoReceiveFrame(
    instanceNumber,
    friendNumber.value,
    width.value,
    height.value,
    y,
    u,
    v,
    yStride,
    uStride,
    vStride
  )

  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

}
