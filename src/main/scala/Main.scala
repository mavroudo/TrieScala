import auth.trie.Trie
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession

object Main {
  def main(args: Array[String]):Unit ={
    val fileName="logTest.withTimestamp"
//    val fileName="BPI Challenge 2017.xes"
//    val fileName="testing.txt"
    Logger.getLogger("org").setLevel(Level.ERROR)
    val spark=SparkSession.builder().appName("auth.trie.Trie scala").master("local[*]").getOrCreate()
    println(s"Starting Spark version ${spark.version}")
//    val data=Utils.readFromTxt(fileName,",")
//    val data=Utils.readFromXes(fileName)
    val data=Utils.readWithTimestamps(fileName,",","/delab/")
    data.collect().foreach(println)
    val trie:Trie=new Trie()
    for (sequence <- data.collect()){
      trie.add(sequence)
    }
    println(trie.get_w())
    println(trie.get_z())
    println(trie.subtree_array('z'))
  }
}
