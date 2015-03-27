import NgAnnotateKeys._
import JsEngineKeys._

lazy val root = (project in file(".")).enablePlugins(SbtWeb)

engineType := JsEngineKeys.EngineType.Node

pipelineStages := Seq(ngAnnotate)
