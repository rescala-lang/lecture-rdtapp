import rdtapp.AppendOnlyList
import rdtapp.AppendOnlyList.given
import rdts.base.LocalUid
import rdts.dotted.Dotted

class Test extends munit.FunSuite {
  test("basic") {
    given LocalUid = LocalUid.gen()

    val d1 = Dotted(AppendOnlyList[String](Map.empty)).mod(_.append("1"))
    val d2 = d1.mod(_.append("2"))

    val d3deltaA = d2.mod(_.append("3a"))
    val d3deltaB = {
      given LocalUid = LocalUid.gen()
      d2.mod(_.append("3b"))
    }

    val d4A = d3deltaA.mod(_.append("4a"))

    assertEquals(d1.data.toList, List("1"))
    assertEquals(
      (d1.merge(d2).merge(d3deltaA).merge(d3deltaB).merge(d4A)).data.toList,
      List("1", "2", "3a", "4a", "3b")
    )
  }
}
