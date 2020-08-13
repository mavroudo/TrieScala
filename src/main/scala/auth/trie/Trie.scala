package auth.trie

import scala.collection.mutable.ListBuffer
import scala.math.Ordered.orderingToOrdered
import scala.math.Ordering.Implicits.seqDerivedOrdering

class Trie extends Serializable {
  private val root = new TrieNode(event_name = "*", "", id = -1)
  private var a_suffix: List[(List[String],List[TrieNode])] = _
  private var a: List[Int] = _

  def add(sequence: Structs.Sequence): Unit = {
    var node = root
    val sequence_id = sequence.id
    for (event <- sequence.events) {
      var foundInChild = false
      val sameEvent: List[TrieNode] = node.getChildren.filter(child => child.getEvent == event.event_name)
      if (sameEvent.nonEmpty) {
        sameEvent.head.visited(sequence_id, event.timestamp)
        node = sameEvent.head
        foundInChild = true
      }
      if (!foundInChild) {
        val new_node: TrieNode = new TrieNode(event.event_name, event.timestamp, sequence_id)
        node.addChildren(new_node)
        node = new_node
      }
    }
    node.setWordFinished(true)
  }

  def find_prefix(sequence: List[Structs.Event]): Structs.FindPrefix = {
    var node = root
    if (node.getChildren.isEmpty) {
      return Structs.FindPrefix(false, 0)
    }
    for (event <- sequence) {
      val next_node = node.getChildren.map(child => {
        (child, event.event_name == child.getEvent)
      }).filter(_._2).map(_._1)
      if (next_node.isEmpty) {
        return Structs.FindPrefix(false, 0)
      }
      else {
        node = next_node.head
      }
    }
    Structs.FindPrefix(true, node.getCounter)
  }

  def _get_w(node: TrieNode): Structs.SequenceWithNodes = {
    var W: ListBuffer[String] = ListBuffer(node.getEvent)
    var W_nodes:ListBuffer[TrieNode]=ListBuffer(node)
    if (node.getWordFinished) {
      return Structs.SequenceWithNodes((W :+ "0").toList,(W_nodes:+null).toList)
    }
    for (n <- node.getChildren) {
      val w=this._get_w(n)
      W = W ++ w.order
      W_nodes=W_nodes++w.nodes
    }
    Structs.SequenceWithNodes((W :+ "0").toList,(W_nodes:+null).toList)
  }

  def get_w(): Structs.SequenceWithNodes = {
    val node = root
    if (root.getChildren.isEmpty) {
      return Structs.SequenceWithNodes(List(root.getEvent),List(null))
    }

    val W = this._get_w(node)
    W
  }

  def get_z(): Structs.SequenceWithNodes = {
    val node = root
    if (root.getChildren.isEmpty) {
      return Structs.SequenceWithNodes(List(root.getEvent),List(null))
    }
    val Z = this._get_z(node)
    Z
  }

  def _get_z(node: TrieNode): Structs.SequenceWithNodes = {
    var Z = ListBuffer(node.getEvent)
    var Z_nodes:ListBuffer[TrieNode]=ListBuffer(node)
    if (node.getWordFinished) {
      return Structs.SequenceWithNodes((Z :+ "0").toList,(Z_nodes:+null).toList)
    }
    for (n <- node.getChildrenSorted()) {
      val z=this._get_z(n)
      Z = Z ++ z.order
      Z_nodes=Z_nodes++z.nodes
    }
    Structs.SequenceWithNodes((Z :+ "0").toList,(Z_nodes:+null).toList)
  }

  def subtree_array(identifier: Char): (List[Int], List[(List[String], List[TrieNode])]) = {
    var order: Structs.SequenceWithNodes = null
    if (identifier == 'z') {
      order = this.get_z()
    } else {
      order = this.get_w()
    }
    val chars = order.order.distinct.filter(s => s != root.getEvent && s != "0").sorted //get the events sorted
    val A: ListBuffer[Int] = ListBuffer[Int]()
    val A_suffix: ListBuffer[(List[String],List[TrieNode])]= ListBuffer[(List[String],List[TrieNode])]()
    for (char <- chars) {
      val suffixes: ListBuffer[(List[String],List[TrieNode])] =ListBuffer [(List[String],List[TrieNode])]()
      val indexes: ListBuffer[Int] = ListBuffer[Int]()
      for (index <- order.order.zipWithIndex.filter(x => x._1 == char).map(_._2)) {
        indexes.append(index)
        val subtree=this._find_subtree(index,order)
        suffixes.append(subtree)
      }
      val zipped = suffixes.zip(indexes).sortBy(_._1._1)
      A_suffix.appendAll(zipped.map(_._1))
      A.appendAll(zipped.map(_._2).toList)
    }
    this.a = A.toList
    this.a_suffix = A_suffix.toList
    (A.toList, A_suffix.toList)
  }

  def _find_subtree(index: Int, sequence: Structs.SequenceWithNodes): (List[String],List[TrieNode]) = {
    var seq: ListBuffer[String] = ListBuffer(sequence.order(index))
    var nodes: ListBuffer[TrieNode] = ListBuffer(sequence.nodes(index))
    var counter: Int = index
    var charCounter = 1
    var zeroCounter = 0
    while (charCounter != zeroCounter) {
      counter += 1
      seq += sequence.order(counter)
      nodes+=sequence.nodes(counter)
      if (sequence.order(counter) == "0") {
        zeroCounter += 1
      } else {
        charCounter += 1
      }
    }
    (seq.toList,nodes.toList)
  }

  def _binarySearchRecursive(list: List[List[String]], target: List[String])
                            (start: Int, end: Int): Int = {
    if (start > end) return -1
    var mid = start + (end - start + 1) / 2
    if (list(mid) == target) {
      while(list(mid)==target){
        if (mid==0){
          return mid
        }
        mid-=1
      }
      mid+1
    }
    else if (list(mid) > target)
      this._binarySearchRecursive(list, target)(start, mid - 1)
    else
      this._binarySearchRecursive(list, target)(mid + 1, end)
  }

  def search_for_tree(order: List[String]): List[(Int, List[String], List[Long])] = {
    val l_events:List[List[String]]=a_suffix.map(f=>f._1)
    val l_nodes:List[List[TrieNode]]=a_suffix.map(f=>f._2)
    var i: Int = this._binarySearchRecursive(l_events, order)(0, a_suffix.size - 1)
    val list: ListBuffer[(Int, List[String], List[Long])] = ListBuffer[(Int, List[String], List[Long])]()
    try {
      if (i != a_suffix.size && l_events(i) == order) {
        while (order == l_events(i)) {
          val unique_nodes_list = l_nodes(i).filter(_ != null).flatMap(_.get_list_of_sequence()).distinct
          list.append((i, l_events(i), unique_nodes_list))
          i += 1
        }
        return list.toList
      }
    }catch {
      case _: IndexOutOfBoundsException => return null
    }
    list.toList
  }

  def execute_query(order: List[String]):List[(Int, List[String], List[Long])] = {
    val query:ListBuffer[String]=ListBuffer[String]()
    query.appendAll(order)
    query.appendAll(order.map(_=>"0"))
    this.search_for_tree(query.toList)
  }

}