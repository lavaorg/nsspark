name := "dpe-spark"
version := "0.0.1"
scalaVersion := "2.10.6"

javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

libraryDependencies ++= {
  val AkkaHttpVersion   = "2.0.5"

  Seq(
    "org.apache.spark" %% "spark-core" % "1.6.1" % "provided",
    "org.apache.spark" %% "spark-sql" % "1.6.1" % "provided",
    "org.apache.spark" %% "spark-repl" % "1.6.1" % "provided",
    "org.apache.spark" %% "spark-streaming" % "1.6.1" % "provided",
    "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016",
    "org.scalatest" % "scalatest_2.10" % "2.0" % "test",
    "com.typesafe.play" %% "play-json" % "2.4.8",
    "org.mockito" % "mockito-core" % "1.9.5",
    "com.typesafe.akka" %% "akka-http-experimental" % AkkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental" % AkkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-testkit-experimental" % AkkaHttpVersion
  )
}
