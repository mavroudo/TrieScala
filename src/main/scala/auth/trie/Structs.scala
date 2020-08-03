package auth.trie

object Structs {

  case class Event(event_name: String, timestamp: String)

  case class Sequence(id: Long, events: List[Event])

  case class FindPrefix(found: Boolean, counter: Int)

}
