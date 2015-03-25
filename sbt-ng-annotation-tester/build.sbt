import NgAnnotateKeys._
import WebJs._
import JsEngineKeys._

lazy val root = (project in file(".")).enablePlugins(SbtWeb)

JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

commandArgs := Seq("--harmony")

pipelineStages := Seq(ngAnnotate)
