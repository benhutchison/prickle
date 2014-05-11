  import sbt._
import sbt.Keys._
import scala.scalajs.sbtplugin.ScalaJSPlugin.ScalaJSKeys._

name := "Prickle-js"

organization := "com.github.benhutchison"

scalaVersion := "2.11.0"

version := "0.1"

unmanagedSourceDirectories in Compile <+= baseDirectory(_ / ".." / "shared" / "main" / "scala")

unmanagedSourceDirectories in Test <+= baseDirectory(_ / ".." / "shared" / "test" / "scala")

autoCompilerPlugins := true

scalaJSSettings

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % "2.11.0",
  "com.lihaoyi" % "utest_2.11" % "0.1.3-JS" % "test"
)

(loadedTestFrameworks in Test) := {
  import scala.scalajs.sbtplugin.ScalaJSPlugin.ScalaJSKeys._
  (loadedTestFrameworks in Test).value.updated(
    sbt.TestFramework(classOf[utest.jsrunner.JsFramework].getName),
    new utest.jsrunner.JsFramework(environment = (scalaJSEnvironment in Test).value)
  )
}

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += Resolver.sonatypeRepo("releases")
