package im.tox.tox4j.impl.jni

import com.google.protobuf.InvalidProtocolBufferException
import im.tox.tox4j.impl.jni.proto.JniLog
import im.tox.tox4j.impl.jni.proto.JniLogEntry
import im.tox.tox4j.impl.jni.proto.Timestamp
import im.tox.tox4j.impl.jni.proto.Value
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

/**
 * The JNI bridge logs every call made to toxcore and toxav functions along
 * with the time taken to execute in microseconds. See the message definitions
 * in ProtoLog.proto to get an idea of what can be done with this log.
 */
object ToxJniLog {

  private val logger = LoggerFactory.getLogger(this::class.java)

  init {
    filterNot(
      "tox_iterate",
      "toxav_iterate",
      "tox_iteration_interval",
      "toxav_iteration_interval"
    )
  }

  fun filterNot(vararg filter: String) {
    ToxCoreJni.tox4jSetLogFilter(filter)
  }

  /**
   * The maximum number of entries in the log. After this limit is reached,
   * logging stops and ignores any further calls until the log is fetched and cleared.
   *
   * Set to 0 to disable logging.
   */
  var maxSize: Int
    get() = ToxCoreJni.tox4jGetMaxLogSize()
    set(value) {
      ToxCoreJni.tox4jSetMaxLogSize(value)
    }


  val size: Int get() = ToxCoreJni.tox4jGetCurrentLogSize()

  /**
   * Retrieve and clear the current call log. Calling [[ToxJniLog]] twice with no
   * native calls in between will return the empty log the second time. If logging
   * is disabled, this will always return the empty log.
   */
  fun apply(): JniLog {
    return fromBytes(ToxCoreJni.tox4jLastLog())
  }

  /**
   * Parse a protobuf message from bytes to [[JniLog]]. Logs an error and returns
   * [[JniLog.defaultInstance]] if $bytes is invalid. Returns [[JniLog.defaultInstance]]
   * if $bytes is null.
   */
  fun fromBytes(bytes: ByteArray?): JniLog {
    return try {
      bytes?.let(JniLog::parseFrom) ?: JniLog.getDefaultInstance()
    } catch (e: InvalidProtocolBufferException) {
      logger.error("${e.message}; unfinishedMessage: ${e.unfinishedMessage}")
      JniLog.getDefaultInstance()
    }
  }

  private fun <A> printDelimited(
    list: Iterable<A>,
    separator: String,
    print: (A, PrintWriter) -> Unit,
    out: PrintWriter,
  ) {
    val i = list.iterator()
    if (i.hasNext()) {
      print(i.next(), out)
      while (i.hasNext()) {
        out.print(separator)
        print(i.next(), out)
      }
    }
  }

  /**
   * Pretty-print the log as function calls with time offset from the first message. E.g.
   * [0.000000] tox_new_unique({udp_enabled=1; ipv6_enabled=0; ...}) [20 µs, #1]
   *
   * The last part is the time spent in the native function followed by the instance number.
   */
  fun print(log: JniLog, out: PrintWriter) {
    log.entriesList.firstOrNull()?.let { first ->
      for (entry in log.entriesList) {
        val timestamp = first.timestamp ?: Timestamp.getDefaultInstance()
        print(timestamp, entry, out)
        out.println()
      }
    }
  }

  private fun printFormattedTimeDiff(a: Timestamp, b: Timestamp, out: PrintWriter) {
    assert(a.nanos < 1000000000)
    assert(b.nanos < 1000000000)

    val seconds = a.seconds - b.seconds
    val nanos = a.nanos - b.nanos

    val timeDiff = if (nanos < 0) {
      Timestamp.newBuilder()
        .setSeconds(seconds - 1)
        .setNanos(nanos + (1.seconds).inWholeNanoseconds.toInt())
        .build()
    } else {
      Timestamp.newBuilder()
        .setSeconds(seconds)
        .setNanos(nanos)
        .build()
    }

    val micros = timeDiff.nanos.nanoseconds.inWholeMicroseconds.toInt()

    out.print(timeDiff.seconds)
    out.print('.')
    out.print(
      when {
        micros < 10 -> "00000"
        micros < 100 -> "0000"
        micros < 1000 -> "000"
        micros < 10000 -> "00"
        micros < 100000 -> "0"
        else -> ""
      }
    )
    out.print(micros)
  }

  fun print(startTime: Timestamp, entry: JniLogEntry, out: PrintWriter) {
    out.print('[')
    printFormattedTimeDiff(entry.timestamp ?: Timestamp.getDefaultInstance(), startTime, out)
    out.print("] ")
    out.print(entry.name)
    out.print('(')
    printDelimited(entry.argumentsList, ", ", ::print, out)
    out.print(") = ")
    print(entry.result ?: Value.getDefaultInstance(), out)
    out.print(" [")

    val elapsedNanos = entry.elapsedNanos.nanoseconds
    val elapsedMicros = elapsedNanos.inWholeMicroseconds

    if (elapsedMicros == 0L) {
      out.print(elapsedNanos)
      out.print(" ns")
    } else {
      out.print(elapsedMicros)
      out.print(" µs")
    }

    if (entry.instanceNumber != 0) {
      out.print(", #")
      out.print(entry.instanceNumber)
    }

    out.print("]")
  }

  fun print(value: Value, out: PrintWriter) {
    when {
      value.hasVBytes() -> {
        out.print("byte[")
        if (value.truncated == 0) {
          out.print(value.vBytes.size())
        } else {
          out.print(value.truncated)
        }
        out.print("]")
      }
      value.hasVObject() -> {
        out.print("{")
        printDelimited(value.vObject.membersMap.toList(), "; ", ::print, out)
        out.print('}')
      }
      value.hasVSint64() -> out.print(value.vSint64)
      value.hasVString() -> out.print(value.vString)
      else -> out.print("void")
    }
  }

  fun print(member: Pair<String, Value>, out: PrintWriter) {
    out.print(member.first)
    out.print("=")
    print(member.second, out)
  }

  fun toString(log: JniLog): String {
    val stringWriter = StringWriter()
    val out = PrintWriter(stringWriter)
    print(log, out)
    out.close()
    return stringWriter.toString()
  }

}
