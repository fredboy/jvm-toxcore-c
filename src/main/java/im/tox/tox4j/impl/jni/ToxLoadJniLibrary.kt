package im.tox.tox4j.impl.jni

import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream

object ToxLoadJniLibrary {

  private val logger = LoggerFactory.getLogger(this::class.java)

  private val ALREADY_LOADED = Regex("Native Library (.+) already loaded in another classloader")
  private val NOT_FOUND_DALVIK =
    Regex("Couldn't load .+ from loader .+ findLibrary returned null")
  private val NOT_FOUND_JVM = Regex("no .+ in java.library.path.*")

  private val lock = Any()

  private fun withTempFile(name: String, block: (File) -> Boolean): Boolean {
    val (prefix, suffix) = name.splitAtLast(".")
    val file = File.createTempFile(prefix, suffix)
    file.deleteOnExit()

    return try {
      block(file)
    } finally {
      // This may fail if the OS doesn't support deleting files that are in use, but deleteOnExit
      // will ensure that it is cleaned up on normal JVM termination.
      file.delete()
    }
  }

  private fun withResources(name: String, block: (InputStream) -> Boolean): Boolean {
    val stream = this::class.java.getResourceAsStream(name)

    return if (stream == null) {
      logger.debug("Resource '$name' not found")
      false
    } else {
      stream.use(block)
    }
  }

  /**
   * Load a native library from an existing location by copying it to a new, temporary location and loading
   * that new library.
   *
   * @param location A [[File]] pointing to the existing library.
   */
  private fun loadFromSystem(location: File): Boolean {
    return withTempFile(location.name) { libraryFile ->
      logger.info("Copying $location to $libraryFile")
      location.copyTo(libraryFile)

      System.load(libraryFile.path)
      true
    }
  }

  /**
   * Load a library from a linked resource jar by copying it to a temporary location and then loading that
   * temporary file.
   *
   * @param name The library name without "dll" suffix or "lib" prefix.
   */
  private fun loadFromJar(name: String): Boolean {
    val osName = mapOf(
      "Mac OS X" to "darwin",
    ).withDefault { key -> key.lowercase().split(" ").firstOrNull() }

    val archName = mapOf(
      "amd64" to "x86_64"
    ).withDefault(String::lowercase)

    val resourceName = "%s-%s/%s".format(
      osName[System.getProperty("os.name").orEmpty()],
      archName[System.getProperty("os.arch").orEmpty()],
      System.mapLibraryName(name)
    )

    logger.debug("Loading $name from resource: $resourceName")

    val location = File(resourceName)
    return withTempFile(location.name) { libraryFile ->
      if (withResources(resourceName) { stream ->
          logger.debug("Copying $resourceName to ${libraryFile.path}")
          libraryFile.writeBytes(stream.readBytes())
          true
        }) {
        System.load(libraryFile.path)
        true
      } else {
        false
      }
    }
  }



  fun load(name: String) = synchronized(lock) {
    try {
      System.loadLibrary(name)
    } catch (e: UnsatisfiedLinkError) {
      logger.debug("Could not load native library '$name' (${e.message}). " +
              "java.library.path = ${System.getProperty("java.library.path")}.")

      val loaded = e.message?.let { message ->
        ALREADY_LOADED.matchEntire(message)?.let { result ->
          logger.warn("$message  copying file and loading again")
          loadFromSystem(File(result.groupValues[1]))
        } ?: NOT_FOUND_JVM.matchEntire(message)?.let {
          loadFromJar(name)
        } ?: NOT_FOUND_DALVIK.matchEntire(message)?.let {
          logger.error("Could not load native library '$name'; giving up.")
          false
        }
      } ?: run {
        logger.error("Unhandled UnsatisfiedLinkError: '${e.message}'")
        false
      }

      if (loaded) {
        logger.debug("Loading '$name' successful")
      } else {
        throw e
      }
    }
  }


  private fun String.splitAtLast(delimiter: String): Pair<String, String> {
    return substringBeforeLast(delimiter) to substringAfterLast(delimiter)
  }


}
