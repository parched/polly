import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.*

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol:

    implicit object BlockJsonFormat extends RootJsonFormat[Block]:
        import java.util.Base64

        val base64encoder = Base64.getEncoder()
        val base64decoder = Base64.getDecoder()

        def toBase64(data: List[Byte]): String =
            base64encoder.encodeToString(data.toArray)

        def fromBase64(data: String): List[Byte] =
            base64decoder.decode(data).toList

        def write(b: Block) =
            JsObject(
                "prev_hash" -> JsString(toBase64(b.prevHash)),
                "data" -> JsString(toBase64(b.data)),
                "modifier" -> JsNumber(b.hashModifier)
            )

        def read(value: JsValue) =
            value.asJsObject.getFields("prev_hash", "data", "modifier") match
                case Seq(
                        JsString(prevHash),
                        JsString(data),
                        JsNumber(modifier)
                    ) =>
                    Block(
                        fromBase64(prevHash),
                        fromBase64(data),
                        modifier.toInt
                    )
                case _ => throw DeserializationException("Block expected")

    implicit val insertBlockFormat: RootJsonFormat[Command.InsertBlock] =
        jsonFormat2(Command.InsertBlock.apply)
