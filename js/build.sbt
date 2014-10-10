import scala.scalajs.sbtplugin.ScalaJSPlugin._

import xerial.sbt.Sonatype.SonatypeKeys._

scalaJSSettings

sonatypeSettings

libraryDependencies += "com.github.benhutchison" %%% "microjson" % "0.1"
