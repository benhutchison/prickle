import sbt._
import Keys._

import scala.scalajs.sbtplugin.env.nodejs.NodeJSEnv

import scala.scalajs.sbtplugin.ScalaJSPlugin.ScalaJSKeys._

object Build extends sbt.Build{
  val cross = new utest.jsrunner.JsCrossBuild(
    organization := "com.github.benhutchison",

    version := "1.0.2",
    scalaVersion := "2.11.2",
    name := "prickle",
    crossScalaVersions := Seq("2.11.2"),

    libraryDependencies += "org.scala-lang" % "scala-reflect" % "2.11.2" % "compile",

    publishArtifact in Test := false,
    publishTo <<= version { (v: String) =>
      Some("releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2")
    },

    scalacOptions ++= Seq("-deprecation"),

    pomExtra :=
      <url>https://github.com/benhutchison/prickle</url>
      <licenses>
        <license>
          <name>Apache license</name>
          <url>http://opensource.org/licenses/Apache-2.0</url>
        </license>
      </licenses>
      <scm>
        <url>git://github.com/benhutchison/prickle.git</url>
      </scm>
      <developers>
        <developer>
          <id>benhutchison</id>
          <name>Ben Hutchison</name>
          <url>https://github.com/benhutchison</url>
        </developer>
      </developers>
  )

  lazy val root = cross.root

  lazy val js = cross.js.settings(
    (jsEnv in Test) := new NodeJSEnv
  )

  lazy val jvm = cross.jvm

  lazy val example = project.aggregate(jvm).dependsOn(jvm)
}
