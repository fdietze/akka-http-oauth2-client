import ReleaseTransformations._

organization := "com.github.dakatsuka"

name := "akka-http-oauth2-client"

crossScalaVersions := Seq("2.12.12", "2.13.3")
scalaVersion := crossScalaVersions.value.last


lazy val akkaVersion = "2.5.31"
lazy val akkaHttpVersion = "10.1.12"
lazy val circeVersion    = "0.13.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream"                 % akkaVersion,
  "com.typesafe.akka" %% "akka-http"                   % akkaHttpVersion,
  "io.circe"          %% "circe-generic"               % circeVersion,
  "io.circe"          %% "circe-parser"                % circeVersion,
  "com.typesafe.akka" %% "akka-http-testkit"           % akkaHttpVersion % "test",
  "org.scalatest"     %% "scalatest"                   % "3.2.0" % "test",
  /* "org.scalamock"     %% "scalamock-scalatest-support" % "3.6.0" % "test" */
)

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "utf-8",
  "-feature",
  "-language:existentials",
  "-language:experimental.macros",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Xcheckinit",
  /* "-Xfatal-warnings", */
  "-Xfuture",
  "-Xlint"
)

/* enablePlugins(ScalafmtPlugin) */

/* scalafmtOnCompile := true */

/* scalafmtTestOnCompile := true */

licenses := Seq("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

homepage := Some(url("https://github.com/dakatsuka/akka-http-oauth2-client"))

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ =>
  false
}

releaseCrossBuild := true

publishTo := Some(
  if (isSnapshot.value) Opts.resolver.sonatypeSnapshots
  else Opts.resolver.sonatypeStaging
)

scmInfo := Some(
  ScmInfo(
    url("https://github.com/dakatsuka/akka-http-oauth2-client"),
    "scm:git@github.com:dakatsuka/akka-http-oauth2-client.git"
  )
)

developers := List(
  Developer(
    id = "dakatsuka",
    name = "Dai Akatsuka",
    email = "d.akatsuka@gmail.com",
    url = url("https://github.com/dakatsuka")
  )
)

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  ReleaseStep(action = Command.process("+publishSigned", _)),
  setNextVersion,
  commitNextVersion,
  ReleaseStep(action = Command.process("+sonatypeRelease", _)),
  pushChanges
)
