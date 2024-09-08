package rdtapp

import rdts.base.{Bottom, Lattice, LocalUid, Uid}

/** Integers are not a great choice to represent a replicated value, as none of the straightforward merge functions seems semantically useful */
object IntegerLattices {

  /** Bad because if two replicas increment, only the larger increment wins. */
  given max: Lattice[Int] = math.max

  /** Same as max, but in the other direction */
  given min: Lattice[Int] = math.min

  /** Bitwise operations really make no sense to use as counters */
  given bitwiseOr: Lattice[Int]  = (x, y) => x | y
  given bitwiseAnd: Lattice[Int] = (x, y) => x & y
}

/** Core idea: Per replica counters */
case class GrowOnlyCounter(entries: Map[Uid, Int]) {

  /** We use immutable values for RDTs, where mutations are represented as new values.
    * To make replication more efficient, the new value only contains the changed part of the state (the updated entry)
    */
  def add(n: Int)(using replicaId: LocalUid): GrowOnlyCounter =
    val current = entries.getOrElse(replicaId.uid, 0)
    GrowOnlyCounter(Map(replicaId.uid -> (current + n)))

  /** Get the semantic value of the counter, by summing all the individual counts at each replica! */
  def value: Int = entries.values.sum
}

object GrowOnlyCounter {
  import rdtapp.IntegerLattices.max
  given Lattice[GrowOnlyCounter] = Lattice.derived

  given Bottom[GrowOnlyCounter] = Bottom.provide(zero)

  def zero: GrowOnlyCounter = GrowOnlyCounter(Map.empty)

  /** main method just for example purposes */
  def main(args: Array[String]): Unit = {

    val start = GrowOnlyCounter.zero

    val r1 = LocalUid.gen()
    val r2 = LocalUid.gen()

    val delta1 = {
      given LocalUid = r1
      start.add(10)
    }

    val delta2 = {
      given LocalUid = r2
      start.add(11)
    }

    val result = start.merge(delta1).merge(delta2)

    val reordered = delta2.merge(delta1).merge(delta1)

    assert(result == reordered)

    println(result.value)

  }
}
