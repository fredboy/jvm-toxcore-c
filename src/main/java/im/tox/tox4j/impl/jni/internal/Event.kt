package im.tox.tox4j.impl.jni.internal

/**
 * Function multiplexer to turn a collection of functions into one.
 *
 * This is a collection of nullary functions returning `Unit` (`() => Unit)`) and is itself also a nullary function
 * returning unit. It can be used to implement events where one can register multiple handlers and selectively
 * unregister them.
 */
internal class Event : () -> Unit {

  private val callbacks = ArrayList<() -> Unit>()

  /**
   * Register a callback to be called on [[apply]].
   *
   * The returned [[Event.Id]] should be considered a linear value. It should only be owned by a single owner and never
   * shared.
   *
   * @param callback A [[Runnable]] instance to be called.
   * @return An [[Event.Id]] that can be used to [[apply]] the callback again.
   */
  infix fun add(callback: () -> Unit): Id {
    callbacks.add(callback)
    return IdImpl(callbacks.size - 1)
  }

  /**
   * Unregister a callback. Requires an [[Event.Id]] from [add].
   *
   * After calling this method, the [[Event.Id]] should be considered consumed. Removing the same event handler twice
   * may result in erroneous behaviour. In particular, if between the two [[-=]] calls there is a [[+=]] call, the event
   * ID may have been reused, and the second call will remove the newly added handler.
   *
   * @param id The callback id object.
   */
  infix fun remove(id: Id) {
    val index = id.value
    if (index != invalidIndex) {
      id.reset()
      callbacks[index] = emptyCallback
      pruneCallbacks()
    }
  }

  private fun pruneCallbacks() {
    callbacks.lastOrNull()?.let { callback ->
      if (callback == emptyCallback) {
        callbacks.removeLast()
        pruneCallbacks()
      }
    }
  }

  /**
   * Invoke all callbacks.
   */
  override operator fun invoke() {
    callbacks.forEach { it.invoke() }
  }

  interface Id {

    /**
     * @return The index in the callbacks list.
     */
    val value: Int

    /**
     * Reset the index to an invalid value.
     */
    fun reset()
  }

  private class IdImpl(private var index: Int) : Id {

    override val value: Int get() = index

    override fun reset() {
      index = invalidIndex
    }

  }

  companion object {
    private const val invalidIndex = -1

    private val emptyCallback = { }
  }

}
