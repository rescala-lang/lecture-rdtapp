package todo

import rdtapp.{ApplicationState, RDTApp}
import rdts.base.Lattice
import rdts.syntax.DeltaBuffer
import reactives.default.*
import reactives.operator.Event.CBR
import reactives.operator.{Event, Fold, Signal}
import replication.DataManager

object AppDataManager {

  val CBR(receivedCallback, dataManager: DataManager[ApplicationState]) = Event.fromCallback {
    DataManager[ApplicationState](RDTApp.replicaId, Event.handle, _ => ())
  }

  def hookup[A: Lattice](
      init: A,
      wrap: A => ApplicationState,
      unwrap: ApplicationState => Option[A]
  )(create: (DeltaBuffer[A], Fold.Branch[DeltaBuffer[A]]) => Signal[DeltaBuffer[A]]) = {
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
