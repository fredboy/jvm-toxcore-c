package im.tox.tox4j.impl

import org.slf4j.LoggerFactory

object ToxImplBase {
  private val logger = LoggerFactory.getLogger(this::class.java)

  fun <ToxCoreState, T : Any> tryAndLog(
    fatal: Boolean,
    state: ToxCoreState,
    eventHandler: T,
    callback: (T, ToxCoreState) -> ToxCoreState,
  ): ToxCoreState {
    return if (!fatal) {
      try {
        callback(eventHandler, state)
      } catch (throwable: Throwable) {
        if (isNonFatal(throwable)) {
          logger.warn(
            "Exception caught while executing ${eventHandler::class.java.name}",
            throwable
          )
          state
        } else {
          throw throwable
        }
      }
    } else {
      callback(eventHandler, state)
    }
  }

  private fun isNonFatal(e: Throwable): Boolean {
    return when (e) {
      is VirtualMachineError,
      is ThreadDeath,
      is InterruptedException,
      is LinkageError -> false
      else -> true
    }
  }
}