package io.buddho.sbt.ngannotate

import java.io.File

import com.typesafe.sbt.jse.{SbtJsEngine, SbtJsTask}
import com.typesafe.sbt.web.SbtWeb
import com.typesafe.sbt.web.pipeline.Pipeline
import sbt._
import spray.json.{JsString, JsArray, JsBoolean, JsObject}

import scala.collection.immutable


object Import {
  val ngAnnotate = TaskKey[Pipeline.Stage]("ng-annotate", "Adds AngularJS annotations to Javascript sources.")

  object NgAnnotateKeys {
    val appDir = SettingKey[File]("ng-annotate-app-dir", "The top level directory that contains your app javascript files.")
    val buildDir = SettingKey[File]("ng-annotate-build-dir", "The target directory for the annotated files.")
    val add = SettingKey[Boolean]("ng-annotate-add")
    val remove = SettingKey[Boolean]("ng-annotate-remove")
    val singleQuotes = SettingKey[Boolean]("ng-annotate-single-quotes", "Use single quotes (') instead of double quotes (\")")
    val regexp = SettingKey[String]("ng-annotate-regexp", "Detect short form myMod.controller(...) if myMod matches regexp.")
    val rename = SettingKey[Array[String]]("ng-annotate-rename", "Rename declarations and annotated references.")
    val sourceMap = SettingKey[Boolean]("ng-annotate-source-map", "Generate an inline source map.")
    val sourceRoot = SettingKey[File]("ng-annotate-source-root", "Set the sourceRoot property of the generated source map.")
  }
}

object SbtNgAnnotate extends AutoPlugin {

  override def requires = SbtJsTask

  override def trigger = AllRequirements

  val autoImport = Import

  import SbtJsEngine.autoImport.JsEngineKeys._
  import SbtJsTask.autoImport.JsTaskKeys._
  import SbtWeb.autoImport._
  import WebKeys._
  import autoImport._
  import NgAnnotateKeys._
  import sbt.Keys._
  import sbt._

  override def projectSettings = Seq(
    appDir := (resourceManaged in ngAnnotate).value / "appdir",
    buildDir := (resourceManaged in ngAnnotate).value / "build",
    excludeFilter in ngAnnotate := HiddenFileFilter,
    includeFilter in ngAnnotate := GlobFilter("*.js"),
    ngAnnotate := runAnnotater.dependsOn(nodeModules in Assets).value,
    add := true,
    remove := false,
    singleQuotes := true,
    regexp := "^[a-zA-Z0-9_\\$\\.\\s]+$",
    sourceMap := false,
    resourceManaged in ngAnnotate := webTarget.value / ngAnnotate.key.label
  )

  private def runAnnotater: Def.Initialize[Task[Pipeline.Stage]] = Def.task {
    mappings =>

      val include = (includeFilter in ngAnnotate).value
      val exclude = (excludeFilter in ngAnnotate).value

      val preMappings = mappings.filter(f => !f._1.isDirectory && include.accept(f._1) && !exclude.accept(f._1))
      SbtWeb.syncMappings(
        streams.value.cacheDirectory,
        preMappings,
        appDir.value
      )

      val cacheDirectory = streams.value.cacheDirectory / ngAnnotate.key.label
      val runUpdate = FileFunction.cached(cacheDirectory, FilesInfo.hash) {
        inputFiles =>
          streams.value.log("Annotating js with ngAnnotate")

          val sourceFileMappings = JsArray(inputFiles.filter(_.isFile).map { f =>
            val relativePath = IO.relativize(appDir.value, f).get
            JsArray(JsString(f.getPath), JsString(relativePath))
          }.toList).toString()

          val targetPath = buildDir.value.getPath

          val jsOptions = JsObject(
            "add" -> JsBoolean(add.value),
            "remove" -> JsBoolean(remove.value),
            "single_quotes" -> JsBoolean(singleQuotes.value),
            "regexp" -> JsString(regexp.value),
            "sourcemap" -> JsBoolean(sourceMap.value)
          ).toString()

          val shellFile = SbtWeb.copyResourceTo(
            (resourceManaged in ngAnnotate).value,
            getClass.getClassLoader.getResource("ng-annotate-shell.js"),
            streams.value.cacheDirectory / "copy-resource"
          )

          SbtJsTask.executeJs(
            state.value,
            (engineType in ngAnnotate).value,
            (command in ngAnnotate).value,
            (nodeModuleDirectories in Plugin).value.map(_.getPath),
            shellFile,
            Seq(sourceFileMappings, targetPath, jsOptions),
            (timeoutPerSource in ngAnnotate).value * preMappings.size
          )

          buildDir.value.***.get.toSet
      }

      val a = appDir.value.***.get.toSet

      val postMappings = runUpdate(a).filter(_.isFile).pair(relativeTo(buildDir.value))
      (mappings.toSet -- preMappings ++ postMappings).toSeq
  }

}
