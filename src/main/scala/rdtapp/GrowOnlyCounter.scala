package rdtapp

import rdts.base.{Bottom, Lattice, LocalUid, Uid}

/** Core idea: Per replica counters, current value is the sum of each counter. */
case class GrowOnlyCounter(/* put inner values of the data type here */) {

  /** We use immutable values for RDTs, where mutations are represented as new values.
    * To make replication more efficient, the new value only contains the changed part of the state (the updated entry)
    */
  def add(n: Int)(using replicaId: LocalUid): GrowOnlyCounter = ???

  /** Get the value of the counter, by summing all the individual counts at each replica */
  def value: Int = ???
}

object GrowOnlyCounter {

  import rdtapp.extra.IntegerLattices.max

  given Lattice[GrowOnlyCounter] = Lattice.derived

  given Bottom[GrowOnlyCounter] = Bottom.provide(zero)

  def zero: GrowOnlyCounter = GrowOnlyCounter()
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

    val reordered = delta2.merge(delta1).merge(delta1).merge(start)

    println(result)
    println(reordered)

    assert(result == reordered)

    println(result.value)

  }
}
