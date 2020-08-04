name := "TrieScala"

version := "0.1"

scalaVersion := "2.11.12"

libraryDependencies += "com.typesafe.scala-logging" % "scala-logging-slf4j_2.10" % "2.1.2"

val sparkVersion = "2.4.4"


//to run the sbt assembly the '% "provided",' section must not be in comments
//to debug in IDE the '% "provided",' section must be in comments
libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion  % "provided",
  "org.apache.spark" %% "spark-mllib" % sparkVersion % "provided",
  "org.apache.spark" %% "spark-sql" % sparkVersion % "provided")

assemblyMergeStrategy in assembly := {
  case manifest if manifest.contains("MANIFEST.MF") =>
    // We don't need manifest files since sbt-assembly will create
    // one with the given settings
    MergeStrategy.discard
  case referenceOverrides if referenceOverrides.contains("reference-overrides.conf") =>
    // Keep the content for all reference-overrides.conf files
    MergeStrategy.concat
  case x =>
    // For all the other files, use the default sbt-assembly merge strategy
    //val oldStrategy = (assemblyMergeStrategy in assembly).value
    //oldStrategy(x)
    MergeStrategy.first
}