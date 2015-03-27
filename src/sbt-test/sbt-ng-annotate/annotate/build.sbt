import JsEngineKeys._

lazy val root = (project in file(".")).enablePlugins(SbtWeb)

isSnapshot := true

engineType := JsEngineKeys.EngineType.Node

pipelineStages := Seq(ngAnnotate)

val checkAnnotations = taskKey[Unit]("check that the js is annotated")

checkAnnotations := {
  def checkJs(name: String) {
    val original = IO.read(file(s"src/main/public/js/$name"))
    val annotated = IO.read(file(s"target/web/stage/js/$name"))
    if (original.size >= annotated.size) {
      sys.error(s"Expected js to be annotated: $name (original: ${original.size} => ${annotated.size}")
    }
  }
  checkJs("app.js")
}