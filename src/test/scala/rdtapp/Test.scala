import rdtapp.extra.AppendOnlyList.given
import rdtapp.extra.AppendOnlyList
import rdts.base.LocalUid
import rdts.syntax.DeltaBuffer

class Test extends munit.FunSuite {
  test("basic") {
    given LocalUid = LocalUid.gen()

    val d1 = DeltaBuffer(AppendOnlyList[String](Map.empty)).mod(_.append("1"))
    val d2 = d1.mod(_.append("2"))

    val d3deltaA = d2.mod(_.append("3a"))
    val d3deltaB = {
      given LocalUid = LocalUid.gen()
      d2.mod(_.append("3b"))
    }

    val d4A = d3deltaA.mod(_.append("4a"))

    assertEquals(d1.state.toList, List("1"))

    val merged = d1.state.merge(d2.state).merge(d3deltaA.state).merge(d3deltaB.state).merge(d4A.state)
    val ids    = merged.toList

    // all five operations must be present and none may be duplicated
    assertEquals(ids.toSet, Set("1", "2", "3a", "3b", "4a"))
    // the order must respect causality: successors always come after their predecessors
    assert(ids.indexOf("1") < ids.indexOf("2"), "1 must appear before 2")
    assert(ids.indexOf("3a") < ids.indexOf("4a"), "3a must appear before its successor 4a")
    // the concurrent branch 3b (created on a different replica) may be placed anywhere after 2
    assert(ids.indexOf("2") < ids.indexOf("3b"), "3b must appear after 2")
  }
}
