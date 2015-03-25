package io.buddho.sbt.ngannotate

import java.io.File

import com.typesafe.sbt.jse.{SbtJsEngine, SbtJsTask}
import com.typesafe.sbt.web.SbtWeb
import com.typesafe.sbt.web.pipeline.Pipeline
import sbt._

import scala.collection.immutable


object Import {
  val ngAnnotate = TaskKey[Pipeline.Stage]("ng-annotate", "Adds AngularJS annotations to Javascript sources.")

  object NgAnnotateKeys {
    val buildDir = SettingKey[File]("ng-annotate-build-dir")
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
    buildDir := (resourceManaged in ngAnnotate).value / "build",
    excludeFilter in ngAnnotate := HiddenFileFilter,
    includeFilter in ngAnnotate := GlobFilter("*.js"),
    singleQuotes := false,
    regexp := "^[a-zA-Z0-9_\\$\\.\\s]+$",
    rename := Array[String](),
    sourceMap := false,
    sourceRoot := (sourceDirectory in ngAnnotate).value,
    ngAnnotate := runNgAnnotate.dependsOn(WebKeys.nodeModules in Assets).value
  )


  private def runNgAnnotate: Def.Initialize[Task[Pipeline.Stage]] = Def.task { mappings =>
    val include = (includeFilter in ngAnnotate).value
    val exclude = (excludeFilter in ngAnnotate).value
    val ngAnnotateMappings = mappings.filter(f => !f._1.isDirectory && include.accept(f._1) && !exclude.accept(f._1))

    SbtWeb.syncMappings(
      streams.value.cacheDirectory,
      ngAnnotateMappings,
      buildDir.value
    )

    val buildMappings = ngAnnotateMappings.map(o => buildDir.value / o._2)

    val cacheDirectory = streams.value.cacheDirectory / ngAnnotate.key.label

    val runUpdate = FileFunction.cached(cacheDirectory, FilesInfo.hash) { inputFiles =>
      streams.value.log.info("NGAnnotate")

      val inputFileArgs = inputFiles.map(_.getPath)

      println(inputFiles)

      val allArgs = Seq()

      SbtJsTask.executeJs(
        state.value,
        (engineType in ngAnnotate).value,
        (command in ngAnnotate).value,
        (nodeModuleDirectories in Assets).value.map(_.getPath),
        (nodeModuleDirectories in Assets).value.last / "ng-annotate" / "ng-annotate",
        allArgs,
        (timeoutPerSource in ngAnnotate).value * ngAnnotateMappings.size,
        commandArgs.value.to[immutable.Seq]
      )

      println(buildDir.value)

      buildDir.value.***.get.filter(!_.isDirectory).toSet
    }

    val ngAnnotatedMappings = runUpdate(buildMappings.toSet).pair(relativeTo(buildDir.value))
    (mappings.toSet -- ngAnnotateMappings ++ ngAnnotatedMappings).toSeq
  }

}
