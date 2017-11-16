name := "stream-benchmarks"

version := "0.1"

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  "commons-io" % "commons-io" % "2.5",
  "io.monix" %% "monix" % "2.3.2",
  // couldn't find 0.0.2 release so built 0.0.3 locally.
  "io.monix" %% "monix-nio" % "0.0.3-SNAPSHOT",
  "co.fs2" %% "fs2-io" % "0.10.0-M8"
)

enablePlugins(JmhPlugin)
