import java.io.{File, FileInputStream}
import java.text.SimpleDateFormat

import auth.trie.Structs
import auth.trie.Structs.Event
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.deckfour.xes.in.XParserRegistry
import org.deckfour.xes.model.{XLog, XTrace}

import scala.collection.JavaConversions.asScalaBuffer

object Utils {

  def readFromTxt(fileName: String, seperator: String): RDD[Structs.Sequence] = {
    val spark = SparkSession.builder().getOrCreate()
    spark.sparkContext.textFile(fileName).zipWithIndex map { case (line, index) =>
      val sequence = line.split(seperator).zipWithIndex map { case (event, inner_index) =>
        Structs.Event(event, inner_index.toString)
      }
      Structs.Sequence(index, sequence.toList)
    }
  }

  def readFromXes(fileName: String): RDD[Structs.Sequence] = {
    val spark = SparkSession.builder().getOrCreate()
    val file_Object = new File(fileName)
    var parsed_logs: List[XLog] = null
    val parsers_iterator = XParserRegistry.instance().getAvailable.iterator()
    while (parsers_iterator.hasNext) {
      val p = parsers_iterator.next
      if (p.canParse(file_Object)) {
        parsed_logs = p.parse(new FileInputStream(file_Object)).toList
      }
    }

    val df = new SimpleDateFormat("MMM d, yyyy HH:mm:ss a") //read this pattern from xes
    val df2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss") // transform it to this patter

    val data = parsed_logs.head.zipWithIndex map { case (trace: XTrace, index: Int) =>
      val list = trace.map(event => {
        val event_name = event.getAttributes.get("concept:name").toString
        val timestamp_occurred = event.getAttributes.get("time:timestamp").toString
        Event(df2.format(df.parse(timestamp_occurred)), event_name)
      }).toList
      Structs.Sequence(index.toLong, list)
    }
    val par = spark.sparkContext.parallelize(data)
    par
  }

  def readWithTimestamps(fileName: String, seperator: String, delimiter: String): RDD[Structs.Sequence] = {
    val spark = SparkSession.builder().getOrCreate()
    spark.sparkContext.textFile(fileName).zipWithIndex map { case (line, index) =>
      val sequence = line.split(seperator).map(event=>{
        print(index + " ")
        println(event)
        Structs.Event(event.split(delimiter)(0),event.split(delimiter)(1))
      })
      Structs.Sequence(index, sequence.toList)
    }
  }

}
