import sbt.Keys._

lazy val root = (project in file("."))
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(scriptedSettings)
  .settings(
    sbtPlugin := true,
    organization := "io.buddho.sbt",
    name := "sbt-ng-annotate",
    git.baseVersion := "0.1.1",
    scalaVersion := "2.10.6",
    scalacOptions += "-feature",
    resolvers ++= Seq(
      "Typesafe Releases Repository" at "http://repo.typesafe.com/typesafe/releases/"
    ),

    addSbtPlugin("com.typesafe.sbt" % "sbt-js-engine" % "1.1.4"),
    addSbtPlugin("com.typesafe.sbt" % "sbt-web" % "1.4.0"),

    scriptedLaunchOpts <+= version apply { v => s"-Dproject.version=$v" },

    bintrayOrganization := Option("buddho"),
    bintrayPackageLabels := Seq("sbt", "sbt-plugin", "ng-annotate"),
    bintrayReleaseOnPublish in ThisBuild := false,
    bintrayRepository := "sbt-plugins",
    publishMavenStyle := false,
    licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
    publish <<= publish dependsOn (test in Test)
  )


