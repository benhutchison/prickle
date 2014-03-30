name := "Prickle"

scalaVersion := "2.10.3"

version := "0.1"

lazy val root = project.in(file("."))

lazy val js = project.in(file("js"))

lazy val playjson = project.in(file("playjson"))

unmanagedSourceDirectories in Compile <+= baseDirectory(_ / "shared" / "main" / "scala")

unmanagedSourceDirectories in Test <+= baseDirectory(_ / "shared" / "test" / "scala")

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % "2.10.3",
  "org.scalamacros" % "quasiquotes_2.10.3" % "2.0.0-M3",
  "com.lihaoyi.utest" % "utest_2.10" % "0.1.1" % "test"
)

addCompilerPlugin("org.scalamacros" % "paradise_2.10.3" % "2.0.0-M3")

testFrameworks += new TestFramework("utest.runner.JvmFramework")

autoCompilerPlugins := true

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += Resolver.sonatypeRepo("releases")

//scalacOptions ++= Seq("-Ymacro-debug-lite")

//scalacOptions ++= Seq("-Xlog-implicits")