package rdtapp.extra

import rdts.base.Lattice

object IntegerLattices {
  given max: Lattice[Int]        = math.max
  given min: Lattice[Int]        = math.min
  given bitwiseOr: Lattice[Int]  = (x, y) => x | y
  given bitwiseAnd: Lattice[Int] = (x, y) => x & y
}
