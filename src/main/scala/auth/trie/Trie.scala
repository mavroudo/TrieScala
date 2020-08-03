package auth.trie

import scala.collection.mutable.ListBuffer
import scala.math.Ordered.orderingToOrdered
import scala.math.Ordering.Implicits.seqDerivedOrdering

class Trie extends Serializable {
  private var root = new TrieNode(event_name = "*", "", id = -1)
  private var a_suffix: List[List[String]] = _
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

  def _get_w(node: TrieNode): List[String] = {
    var W: ListBuffer[String] = ListBuffer(node.getEvent)
    if (node.getWordFinished) {
      return (W :+ "0").toList
    }
    for (n <- node.getChildren) {
      W = W ++ this._get_w(n)
    }
    (W :+ "0").toList
  }

  def get_w(): List[String] = {
    val node = root
    if (root.getChildren.isEmpty) {
      return List(root.getEvent)
    }
    val W = this._get_w(node)
    W
  }

  def get_z(): List[String] = {
    val node = root
    if (root.getChildren.isEmpty) {
      return List(root.getEvent)
    }
    val Z = this._get_z(node)
    Z
  }

  def _get_z(node: TrieNode): List[String] = {
    var Z = ListBuffer(node.getEvent)
    if (node.getWordFinished) {
      return (Z :+ "0").toList
    }
    for (n <- node.getChildrenSorted()) {
      Z = Z ++ this._get_z(n)
    }
    (Z :+ "0").toList
  }

  def subtree_array(identifier: Char): (List[Int], List[List[String]]) = {
    var order: List[String] = null
    if (identifier == 'z') {
      order = this.get_z()
    } else {
      order = this.get_w()
    }
    val chars = order.distinct.filter(s => s != root.getEvent && s != "0").sorted
    val A: ListBuffer[Int] = ListBuffer[Int]()
    val A_suffix: ListBuffer[List[String]] = ListBuffer[List[String]]()
    for (char <- chars) {
      val suffixes: ListBuffer[List[String]] = ListBuffer[List[String]]()
      val indexes: ListBuffer[Int] = ListBuffer[Int]()
      for (index <- order.zipWithIndex.filter(x => x._1 == char).map(_._2)) {
        indexes.append(index)
        suffixes.append(this._find_subtree(index, order))
      }
      val zipped = suffixes.zip(indexes).sortBy(_._1)
      A_suffix.appendAll(zipped.map(_._1))
      A.appendAll(zipped.map(_._2).toList)
    }
    this.a = A.toList
    this.a_suffix = A_suffix.toList
    (A.toList, A_suffix.toList)

  }

  def _find_subtree(index: Int, sequence: List[String]): List[String] = {
    var seq: ListBuffer[String] = ListBuffer(sequence(index))
    var counter: Int = index
    var charCounter = 1
    var zeroCounter = 0
    while (charCounter != zeroCounter) {
      counter += 1
      seq += sequence(counter)
      if (sequence(counter) == "0") {
        zeroCounter += 1
      } else {
        charCounter += 1
      }
    }
    seq.toList
  }

  def _binarySearchRecursive(list: List[List[String]], target: List[String])
                            (start: Int, end: Int): Int = {
    if (start > end) return -1
    val mid = start + (end - start + 1) / 2
    if (list(mid) == target)
      mid
    else if (list(mid) > target)
      this._binarySearchRecursive(list, target)(start, mid - 1)
    else
      this._binarySearchRecursive(list, target)(mid + 1, end)
  }

  def search_for_tree(order: List[String]): List[(Int, List[String])] = {
    var i: Int = this._binarySearchRecursive(a_suffix, order)(0, a_suffix.size - 1)
    val list: ListBuffer[(Int, List[String])] = ListBuffer[(Int, List[String])]()
    if (i != a_suffix.size && a_suffix(i) == order) {
      while (order == a_suffix(i)) {
        list.append((i, a_suffix(i)))
        i += 1
      }
      return list.toList
    }
    list.toList
  }

}