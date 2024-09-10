package rdtapp

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.{CodecMakerConfig, JsonCodecMaker}
import rdts.base.{Bottom, Lattice, LocalUid}
import rdts.datatypes.LastWriterWins

object ApplicationState {
  given Lattice[ApplicationState] = Lattice.derived

  // needed by the replication manager to handle some special cases
  given bottom: Bottom[ApplicationState] = Bottom.provide(ApplicationState())

  // this is for serialization to JSON
  given codec: JsonValueCodec[ApplicationState] = JsonCodecMaker.make(CodecMakerConfig.withMapAsArray(true))

}

case class ApplicationState(
    message: LastWriterWins[String] = LastWriterWins.fallback(""),
    upvotes: GrowOnlyCounter = GrowOnlyCounter.zero,
    downvotes: GrowOnlyCounter = GrowOnlyCounter.zero
) {
  def like()(using LocalUid): ApplicationState =
    ApplicationState(upvotes = upvotes.add(1))

  def dislike()(using LocalUid): ApplicationState =
    ApplicationState(downvotes = downvotes.add(1))

  def setMessage(newMessage: String): ApplicationState =
    ApplicationState(message = message.write(newMessage))
}
