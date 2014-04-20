name := "prickle play-json"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += Resolver.sonatypeRepo("releases")

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % "2.10.3",
  "org.scalamacros" % "quasiquotes_2.10.3" % "2.0.0-M3",
  "com.typesafe.play" %% "play-json" % "2.2.1",
  "com.lihaoyi.utest" % "utest_2.10" % "0.1.1" % "test"
)

testFrameworks += new TestFramework("utest.runner.JvmFramework")


addCompilerPlugin("org.scalamacros" % "paradise_2.10.3" % "2.0.0-M3")

autoCompilerPlugins := true

unmanagedSourceDirectories in Compile <+= baseDirectory(_ / ".." / "shared" / "main" / "scala")

unmanagedSourceDirectories in Test <+= baseDirectory(_ / ".." / "shared" / "test" / "scala")
