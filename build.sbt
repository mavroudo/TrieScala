name := "TrieScala"

version := "0.1"

scalaVersion := "2.11.12"

libraryDependencies += "com.typesafe.scala-logging" % "scala-logging-slf4j_2.10" % "2.1.2"

val sparkVersion = "2.4.4"


//to run the sbt assembly the '% "provided",' section must not be in comments
//to debug in IDE the '% "provided",' section must be in comments
libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion ,
  "org.apache.spark" %% "spark-mllib" % sparkVersion,
  "org.apache.spark" %% "spark-sql" % sparkVersion )
