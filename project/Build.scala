import sbt._
import Keys._

import xerial.sbt.Sonatype.SonatypeKeys._
import xerial.sbt.Sonatype.sonatypeSettings


import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import org.scalajs.sbtplugin.cross.CrossProject

object Build extends sbt.Build{

  lazy val sharedSettings = Seq(
    organization := "com.github.benhutchison",

    version := "1.1.11",

    scalaVersion := "2.11.8",

    name := "prickle",

    libraryDependencies ++= Seq( "org.scala-lang" % "scala-reflect" % scalaVersion.value % "compile"  ),

    publishArtifact in Test := false,
    publishTo <<= version { (v: String) =>
      Some("releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2")
    },

    testFrameworks += new TestFramework("utest.runner.Framework"),

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
  ) ++ sonatypeSettings

  lazy val cross = CrossProject("prickle",new File("."),CrossType.Full).
    settings(
      sharedSettings: _*).
    jsSettings(
      libraryDependencies += "com.github.benhutchison" %%% "microjson" % "1.3",
      libraryDependencies += "com.lihaoyi" %%% "utest" % "0.4.3" % "test"
    ).
    jvmSettings(
      libraryDependencies += "com.github.benhutchison" %% "microjson" % "1.3",
      libraryDependencies += "com.lihaoyi" %% "utest" % "0.4.3" % "test"
    )

  lazy val root = project.in(file(".")).aggregate(js, jvm).
    settings(
      publishArtifact := false,
      crossScalaVersions := Seq("2.11.8"),
      sonatypeProfileName := "com.github.benhutchison"
    )


  lazy val js = cross.js
  lazy val jvm   = cross.jvm
  lazy val example = Project(
    id = "example",
    base = file("example")
  ).aggregate(jvm).dependsOn(jvm)
}
