lazy val sharedSettings = Seq(
    organization := "com.github.benhutchison",

    version := "1.1.16",

    scalaVersion := "2.13.2",

    name := "prickle",

    libraryDependencies ++= Seq(
      "com.github.benhutchison" %%% "microjson" % "1.6",
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % "compile",
      "com.lihaoyi" %%% "utest" % "0.7.4" % "test"
     ),

    publishArtifact in Test := false,

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
  )

  lazy val cross = crossProject(JSPlatform, JVMPlatform).in(file(".")).settings(sharedSettings: _*)

  lazy val root = project.in(file(".")).aggregate(js, jvm).
    settings(
      publishArtifact := false,
      crossScalaVersions := Seq("2.13.2"),
      sonatypeProfileName := "com.github.benhutchison"
    )

ThisBuild / publishTo := Some(Opts.resolver.sonatypeStaging)
ThisBuild / isSnapshot := true


  lazy val js = cross.js
  lazy val jvm   = cross.jvm
  lazy val example = Project(
    id = "example",
    base = file("example")
  ).settings(scalaVersion := "2.13.2").aggregate(jvm).dependsOn(jvm)