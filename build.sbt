

sbtPlugin := true

organization := "io.buddho.sbt"

name := "sbt-ng-annotate"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.10.4"

scalacOptions += "-feature"

resolvers ++= Seq(
  "Typesafe Releases Repository" at "http://repo.typesafe.com/typesafe/releases/",
  Resolver.url("sbt snapshot plugins", url("http://repo.scala-sbt.org/scalasbt/sbt-plugin-snapshots"))(Resolver.ivyStylePatterns),
  Resolver.sonatypeRepo("snapshots"),
  "Typesafe Snapshots Repository" at "http://repo.typesafe.com/typesafe/snapshots/",
  Resolver.mavenLocal
)

addSbtPlugin("com.typesafe.sbt" % "sbt-js-engine" % "1.0.2")

publishMavenStyle := false

publishTo := {
  if (isSnapshot.value) Some(Classpaths.sbtPluginSnapshots)
  else Some(Classpaths.sbtPluginReleases)
}

scriptedSettings

scriptedLaunchOpts <+= version apply { v => s"-Dproject.version=$v" }

publishMavenStyle := true

publishTo := {
  if (isSnapshot.value) Some("snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")
  else Some("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2")
}

pomExtra :=
  <url>https://github.com/buddho-io/sbt-ng-annotate</url>
    <licenses>
      <license>
        <name>Apache License, Version 2.0</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:buddho-io/sbt-ng-annotate.git</url>
      <connection>scm:git:git@github.com:buddho-io/sbt-ng-annotate.git</connection>
    </scm>
    <developers>
      <developer>
        <id>llinder</id>
        <name>Lance Linder</name>
        <url>https://github.com/llinder</url>
      </developer>
    </developers>
