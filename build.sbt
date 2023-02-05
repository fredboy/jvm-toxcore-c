// General settings.
organization  := "org.toktok"
name          := "tox4j-c"
version       := "0.2.3"
scalaVersion  := "2.12.17"

publishMavenStyle := true
publishM2Configuration := publishM2Configuration.value.withOverwrite(true)

/******************************************************************************
 * Dependencies
 ******************************************************************************/
resolvers += Resolver.mavenLocal

// Build dependencies.
libraryDependencies ++= Seq(
  "org.toktok" %% "tox4j-api" % version.value,
  "org.toktok" %% "macros" % "0.1.1",
  "com.chuusai" %% "shapeless" % "2.3.10",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
  "com.google.guava" % "guava" % "23.0"
)

// Test dependencies.
libraryDependencies ++= Seq(
  "com.storm-enroute" %% "scalameter" % "0.19",
  "org.jline" % "jline" % "3.22.0",
  "junit" % "junit" % "4.13.2",
  "org.scalatest" %% "scalatest" % "3.2.15",
  "org.scalaz" %% "scalaz-concurrent" % "7.3.0-M27",
  "org.slf4j" % "slf4j-log4j12" % "2.0.5",
  "org.scalatestplus" %% "scalacheck-1-17" % "3.2.15.0",
  "org.scalatestplus" %% "junit-4-13" % "3.2.15.0"
) map (_ % Test)

// Add ScalaMeter as test framework.
testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework")

// Disable parallel test execution, as network tests become flaky that way.
parallelExecution in Test := false

/******************************************************************************
 * Other settings and plugin configuration.
 ******************************************************************************/

// TODO(iphydf): Require less test coverage for now, until ToxAv is tested.
import scoverage.ScoverageKeys._
coverageMinimum := 20
coverageExcludedPackages := ".*\\.proto\\..*"

import im.tox.sbt.Scalastyle
Scalastyle.projectSettings

// Mixed project.
compileOrder := CompileOrder.Mixed
Compile / scalaSource := (Compile / javaSource).value
Test / scalaSource := (Test / javaSource).value

// Override Scalastyle configuration for test.
scalastyleConfigUrl in Test := None
scalastyleConfig in Test := (scalaSource in Test).value / "scalastyle-config.xml"

Compile / PB.targets := Seq(
  scalapb.gen(flatPackage = true, javaConversions = true, grpc = true) -> (Compile / sourceManaged).value / "scalapb",
  PB.gens.java -> (Compile / sourceManaged).value / "scalapb"
)
