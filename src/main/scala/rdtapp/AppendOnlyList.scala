package rdtapp

import rdts.base.{Bottom, Lattice, LocalUid}
import rdts.dotted.{Dotted, HasDots}
import rdts.time.{Dot, Dots}

import scala.collection.mutable.ListBuffer

/* In a purely local setting (i.e., a shared address space) a dag might refer to predecessors by object references,
 * and the identity of the DAG nodes is given by by the object identity. */
case class AppendOnlyLocalNode[T](value: T, predecessors: Set[AppendOnlyLocalNode[T]])

/* Memory references into the heap don’t work well in a distributed setting, and neither do object identities.
 * We instead add explicit uids in the form of `Dot`s, and refer to predecessors by their dot.
 * This requires us to somehow assign unique dots to each entry – we will see how to do that soon. */
case class AppendOnlyNode[T](value: T, predecessors: Set[Dot])

/* An append only list can then be represented by a set of dots – we wrap the set in a custom data structure to add custom operations */
case class AppendOnlyList[T](entries: Map[Dot, AppendOnlyNode[T]]) {

  lazy val toList: List[T] = {
    val graph = entries.view.mapValues(_.predecessors).toMap
    val order = Util.deterministicToposort(graph)
    order.map(dot => entries(dot).value)
  }

  lazy val roots: Set[Dot] = {
    val referredTo = entries.values.flatMap(entry => entry.predecessors).toSet
    entries.keys.filter(dot => !referredTo.contains(dot)).toSet
  }

  def append(value: T)(using context: Dots, replicaId: LocalUid): Dotted[AppendOnlyList[T]] = {
    val nextDot = context.nextDot(replicaId.uid)
    Dotted(AppendOnlyList(Map(nextDot -> AppendOnlyNode(value, roots))), Dots.single(nextDot))
  }
}

object AppendOnlyList {
  given lattice[T]: Lattice[AppendOnlyList[T]] = {
    given Lattice[AppendOnlyNode[T]] = Lattice.assertEquals
    Lattice.derived
  }

  given hasDots[T]: HasDots[AppendOnlyList[T]] = HasDots.derived
  given bottom[T]: Bottom[AppendOnlyList[T]] = Bottom.derived
}


object Util {
  def deterministicToposort(graph: Map[Dot, Set[Dot]]): List[Dot] = {
    val seen = collection.mutable.Set[Dot]()
    val sorted = ListBuffer[Dot]()

    def toposortInner(next: Dot): Unit = {
      if seen.contains(next) then ()
      else {
        seen += next
        val predecessors = graph(next)
        predecessors.toList.sortBy(dot => (dot.place, dot.time)).foreach(toposortInner)
        sorted += next
        ()
      }
    }

    graph.keys.toList.sortBy(dot => (dot.place, dot.time)).foreach(toposortInner)
    sorted.result()
  }
}