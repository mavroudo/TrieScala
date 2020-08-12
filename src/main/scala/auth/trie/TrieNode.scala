package auth.trie

import scala.collection.mutable.ListBuffer

class TrieNode(event_name: String, event_timestamp: String, id: Long) extends Serializable {
  private val children = ListBuffer[TrieNode]()
  private var word_finished = false
  private var counter = 1
  private var list_of_sequences = List[Long]() :+ id
  private var list_of_timestamps = List[String]() :+ event_timestamp

  def getChildren: List[TrieNode] = {
    children.toList
  }

  override def toString: String = {
    val string: StringBuilder = new StringBuilder()
    string.append("Event: " + this.event_name)
    string.append(" List of sequences: " + this.list_of_sequences + " List of timestmaps: " + this.list_of_timestamps)
    string.toString()
  }

  def addChildren(node: TrieNode): Unit = {
    children.append(node)
  }

  def getWordFinished: Boolean = {
    word_finished
  }

  def setWordFinished(isFinished: Boolean): Unit = {
    word_finished = isFinished
  }

  def getEvent: String = {
    event_name
  }

  def getCounter: Int = {
    counter
  }

  def getChildrenSorted(): List[TrieNode] = {
    children.sortBy(x => x.getEvent).toList
  }

  def visited(id: Long, event_timestamp: String): Unit = {
    counter += 1
    list_of_sequences = list_of_sequences :+ id
    list_of_timestamps = list_of_timestamps :+ event_timestamp
  }

  def get_list_of_sequence():List[Long]={
    this.list_of_sequences
  }
}
