//import scala.scalajs.sbtplugin.ScalaJSPlugin

//import xerial.sbt.Sonatype.SonatypeKeys._

lazy val root = project.in(file(".")).enablePlugins(ScalaJSPlugin)

//sonatypeSettings

libraryDependencies += "com.github.benhutchison" %%% "microjson" % "1.0"
