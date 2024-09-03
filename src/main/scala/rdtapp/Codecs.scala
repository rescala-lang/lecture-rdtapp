package rdtapp

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonKeyCodec, JsonReader, JsonValueCodec, JsonWriter}
import com.github.plokhotnyuk.jsoniter_scala.macros.{CodecMakerConfig, JsonCodecMaker}
import rdts.base.Uid
import rdts.datatypes.TwoPhaseSet
import rdts.datatypes.contextual.ReplicatedList
import rdts.dotted.Dotted
import rdts.time.{Dot, Dots}

object Codecs {

  given stringCodec: JsonValueCodec[String] = JsonCodecMaker.make[String]

  // The cast here is because Uid actually is a string … but it is an opaque type so we should not be able to know that.
  // We do need to know what the actual datatype is for the codec though
  given uidCodec: JsonValueCodec[Uid] = stringCodec.asInstanceOf[JsonValueCodec[Uid]]

  given listCodec: JsonValueCodec[List[Chatline]] = JsonCodecMaker.make[List[Chatline]]

  given dottedListCodec: JsonValueCodec[Dotted[ReplicatedList[Chatline]]] =
    JsonCodecMaker.make[Dotted[ReplicatedList[Chatline]]](CodecMakerConfig.withMapAsArray(true))

}
