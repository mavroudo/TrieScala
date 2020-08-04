package auth.trie

import org.apache.log4j.{Level, Logger}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

object Main {
  def main(args: Array[String]): Unit = {
    //    val fileName="logTest.withTimestamp"
    //    val fileName="BPI Challenge 2017.xes"
    //    val fileName="testing.txt"
    val fileName = args(0)
    println(fileName)
    Logger.getLogger("org").setLevel(Level.ERROR)
    val spark = SparkSession.builder().getOrCreate()
    println(s"Starting Spark version ${spark.version}")
    var dataSeq: RDD[Structs.Sequence] = null
    fileName.split('.')(1) match {
      case "txt" => dataSeq = Utils.readFromTxt(fileName, ",")
      case "xes" => dataSeq = Utils.readFromXes(fileName)
      case "withTimestamp" => dataSeq = Utils.readWithTimestamps(fileName, ",", "/delab/")
    }
    val trie: Trie = new Trie()
    for (sequence <- dataSeq.collect()) {
      trie.add(sequence)
    }
//    println(trie.subtree_array('z'))
  }
}
