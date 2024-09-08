package rdtapp.helpers

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonReader, JsonValueCodec, JsonWriter}
import com.github.plokhotnyuk.jsoniter_scala.macros.{CodecMakerConfig, JsonCodecMaker}
import rdtapp.ApplicationState
import rdts.base.Uid

object Codecs {

  given stringCodec: JsonValueCodec[String] = JsonCodecMaker.make[String]

  // The cast here is because Uid actually is a string … but it is an opaque type so we should not be able to know that.
  // We do need to know what the actual datatype is for the codec though
  given uidCodec: JsonValueCodec[Uid] = stringCodec.asInstanceOf[JsonValueCodec[Uid]]

}
