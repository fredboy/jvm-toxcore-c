resolvers += Resolver.mavenLocal

// Common tox4j build rules.
addSbtPlugin("org.toktok" % "sbt-plugins" % "0.1.6")

// Compiler version for additional plugins in this project.
scalaVersion  := "2.12.17"

// Build dependencies.
libraryDependencies ++= Seq(
  "com.github.os72" % "protoc-jar" % "3.11.4"
)
