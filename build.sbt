name := "Prickle"

scalaVersion := "2.11.0"

version := "0.1"

lazy val root = project.in(file("."))

lazy val js = project.in(file("js"))

lazy val playjson = project.in(file("playjson"))

unmanagedSourceDirectories in Compile <+= baseDirectory(_ / "shared" / "main" / "scala")

unmanagedSourceDirectories in Test <+= baseDirectory(_ / "shared" / "test" / "scala")

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % "2.11.0",
  "com.lihaoyi" % "utest_2.11" % "0.1.3" % "test"
)

testFrameworks += new TestFramework("utest.runner.JvmFramework")

autoCompilerPlugins := true

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += Resolver.sonatypeRepo("releases")

//scalacOptions ++= Seq("-Ymacro-debug-lite")

//scalacOptions ++= Seq("-Xlog-implicits")