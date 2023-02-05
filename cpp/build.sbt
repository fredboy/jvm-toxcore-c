// General settings.
organization  := "org.toktok"
name          := "tox4j-c_" + sys.env("TOX4J_PLATFORM")
version       := "0.2.13"

// Pure Java project.
crossPaths := false
autoScalaLibrary := false

publishMavenStyle := true
publishM2Configuration := publishM2Configuration.value.withOverwrite(true)
