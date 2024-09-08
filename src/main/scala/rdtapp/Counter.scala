package rdtapp

import rdts.base.{Bottom, Lattice, LocalUid, Uid}

object IntegerLattices {

  given max: Lattice[Int]        = math.max
  given min: Lattice[Int]        = math.min
  given bitwiseOr: Lattice[Int]  = (x, y) => x | y
  given bitwiseAnd: Lattice[Int] = (x, y) => x & y

}

/** Core idea: Per replica counters */
case class GrowOnlyCounter(entries: Map[Uid, Int]) {

  /** We use immutable values for RDTs, where mutations are represented as new values.
    * To make replication more efficient, the new value only contains the changed part of the state (the updated entry)
    */
  def add(n: Int)(using replicaId: LocalUid): GrowOnlyCounter =
    val currentValue   = entries.getOrElse(replicaId.uid, 0)
    val increasedValue = currentValue + n
    GrowOnlyCounter(Map(replicaId.uid -> increasedValue))

  /** Get the semantic value of the counter, by summing all the individual counts at each replica! */
  def value: Int = entries.values.sum
}

object GrowOnlyCounter {

  import rdtapp.IntegerLattices.max

  given Lattice[GrowOnlyCounter] = Lattice.derived

  given Bottom[GrowOnlyCounter] = Bottom.provide(zero)

  def zero: GrowOnlyCounter = GrowOnlyCounter(Map.empty)
}

object Examples {

  def test() = {

    given Lattice[Int] = math.max

    val x: 10 = 10

    val delta1: Int = x + 2

    val delta2: Int = x + 5

    val result: Int = delta1.merge(delta2)

  }

  /** main method just for example purposes */
  def main(args: Array[String]): Unit = {

    val start = GrowOnlyCounter.zero.add(10)(using Uid.zero)

    val r1 = LocalUid.gen()
    val r2 = LocalUid.gen()

    val delta1 = {
      given LocalUid = r1
      start.add(2)
    }

    val delta2 = {
      given LocalUid = r2
      start.add(5)
    }

    val result = start.merge(delta1).merge(delta2)

    val reordered = delta2.merge(delta1).merge(delta1)

    assert(result == reordered)

    println(result.value)

  }
}
