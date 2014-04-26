name := "prickle play-json"

scalaVersion := "2.11.0"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += Resolver.sonatypeRepo("releases")

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % "2.11.0",
  "com.typesafe.play" % "play-json_2.10" % "2.2.1",
  "com.lihaoyi" % "utest_2.11" % "0.1.3" % "test"
)

testFrameworks += new TestFramework("utest.runner.JvmFramework")

autoCompilerPlugins := true

unmanagedSourceDirectories in Compile <+= baseDirectory(_ / ".." / "shared" / "main" / "scala")

unmanagedSourceDirectories in Test <+= baseDirectory(_ / ".." / "shared" / "test" / "scala")
