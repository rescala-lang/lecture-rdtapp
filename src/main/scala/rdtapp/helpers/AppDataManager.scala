package todo

import rdtapp.{MiniSocial, MainUI}
import rdts.base.Lattice
import rdts.syntax.DeltaBuffer
import reactives.operator.Event.CBR
import reactives.operator.{Event, Fold, Signal}
import replication.{DataManager, ProtocolDots}

object AppDataManager {

  val (
    receivedCallback: Event[MiniSocial],
    allCallback: Event[ProtocolDots[MiniSocial]],
    dataManager: DataManager[MiniSocial]
  ) = {
    val CBR(receivedCB, (allCB, dm)) = Event.fromCallback {
      val outerCb = Event.handle[MiniSocial]
      val CBR(allcb, dm) = Event.fromCallback {
        val innerCB = Event.handle[ProtocolDots[MiniSocial]]
        DataManager[MiniSocial](MainUI.replicaId, outerCb, innerCB)
      }
      (allcb, dm)
    }
    (receivedCB, allCB, dm)
  }

  def hookup(init: MiniSocial)(create: (
      DeltaBuffer[MiniSocial],
      Fold.Branch[DeltaBuffer[MiniSocial]]
  ) => Signal[DeltaBuffer[MiniSocial]]): Signal[DeltaBuffer[MiniSocial]] =
    hookup(init, identity, Some.apply)(create)

  def hookup[A: Lattice](
      init: A,
      wrap: A => MiniSocial,
      unwrap: MiniSocial => Option[A]
  )(create: (DeltaBuffer[A], Fold.Branch[DeltaBuffer[A]]) => Signal[DeltaBuffer[A]]): Signal[DeltaBuffer[A]] = {
    dataManager.lock.synchronized {
      dataManager.applyUnrelatedDelta(wrap(init))
      val fullInit = dataManager.allDeltas.flatMap(v => unwrap(v.data)).foldLeft(init)(Lattice.merge)

      val branch = Fold.branch[DeltaBuffer[A]] {
        receivedCallback.value.flatMap(unwrap) match
          case None    => Fold.current
          case Some(v) => Fold.current.applyDeltaNonAppend(v)
      }

      val sig = create(DeltaBuffer(fullInit), branch)

      sig.observe { buffer =>
        buffer.deltaBuffer.foreach { delta =>
          dataManager.applyUnrelatedDelta(wrap(delta))
        }
      }

      sig
    }
  }

}
