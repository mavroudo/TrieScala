package auth.trie

import org.apache.log4j.{Level, Logger}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

import scala.collection.mutable.ListBuffer

object Main {
  def main(args: Array[String]): Unit = {
    //    val fileName="logTest.withTimestamp"
    //    val fileName="BPI Challenge 2017.xes"
    //    val fileName="testing.txt"
    val fileName = args(0)
    val queryFile= args(1)
    Logger.getLogger("org").setLevel(Level.ERROR)
    val spark = SparkSession.builder()
//      .master("local[*]")
      .getOrCreate()
    println(s"Starting Spark version ${spark.version}")
    var dataSeq: RDD[Structs.Sequence] = null
    fileName.split('.')(1) match {
      case "txt" => dataSeq = Utils.readFromTxt(fileName, ",")
      case "xes" => dataSeq = Utils.readFromXes(fileName)
      case "withTimestamp" => dataSeq = Utils.readWithTimestamps(fileName, ",", "/delab/")
    }
    println("Start building trie")
    val trie: Trie = new Trie()
    Utils.time {
      for (sequence <- dataSeq.collect()) {
        trie.add(sequence)
      }
    }
    println("Create subtrees array")
    Utils.time {
      trie.subtree_array('z')
    }
    //read data queries from a file and execute them here
    val queries = Utils.read_queries(queryFile)
    val responses: ListBuffer[List[(Int, List[String], List[Long])]] = ListBuffer[List[(Int, List[String], List[Long])]]()
    println("Executing Queries")
    Utils.time {
      for (q <- queries.collect()) {
        responses.append(trie.execute_query(q))
      }
    }
    Utils.writeFile("queries.out", responses.toList)

  }
}
