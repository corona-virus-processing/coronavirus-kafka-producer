name := "coronavirus-kafka-producer"

version := "0.1"
val akkaVersion = "2.6.15"
val akkaHttpVersion = "10.2.4"


assembly/mainClass := Some("com.corona.Application")
scalaVersion := "2.13.6"

libraryDependencies += "commons-logging" % "commons-logging" % "1.2"
libraryDependencies += "commons-io" % "commons-io" % "20030203.000550"

libraryDependencies += "com.typesafe" % "config" % "1.4.1"
libraryDependencies += "org.apache.kafka" %% "kafka" % "2.8.0"
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.32"
libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.32"
libraryDependencies += "net.liftweb" %% "lift-json" % "3.4.3"



libraryDependencies ++= Seq(
"com.typesafe.akka" %% "akka-stream" % akkaVersion,
"com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
"com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
"com.typesafe.akka" %% "akka-actor-typed" % akkaVersion

)
assembly/assemblyMergeStrategy:= {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case PathList("reference.conf") => MergeStrategy.concat
  case x => MergeStrategy.first
}








