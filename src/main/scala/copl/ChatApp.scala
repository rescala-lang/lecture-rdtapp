package copl

import copl.Codecs.given
import org.scalajs.dom.html.{Div, Input, Paragraph}
import org.scalajs.dom.{UIEvent, document, window}
import rdts.base.LocalUid
import rdts.datatypes.contextual.ReplicatedList
import rdts.dotted.Dotted
import reactives.operator.Fold
import reactives.operator.Fold.current
import scalatags.JsDom.all.*
import scalatags.JsDom.tags2.section
import scalatags.JsDom.{Attr, TypedTag}
import reactives.extra.Tags.{reattach, given}

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

case class Chatline(name: String, message: String)

@JSExportTopLevel("ChatApp")
object ChatApp {


  given LocalUid = LocalUid.gen()

  @JSExport("start")
  def start(): Unit = {
    val content = getContents()

    document.body.replaceChild(content, document.body.firstChild)

    document.body.appendChild(p(style := "height: 3em").render)
  }

  def getContents() = {

    val nameTag                      = input(placeholder := "<your name>")
    val (nameEvent, nameData: Input) = RenderUtil.inputFieldHandler(nameTag, oninput, clear = false)
    val nameS =
      Storing.storedAs("name", "<unnamed>") { init =>
        nameData.value = init
        nameEvent.hold(init)
      }

    val messageTag                  = input("<your message>")
    val (messageEvent, messageData) = RenderUtil.inputFieldHandler(messageTag, onchange, clear = true)

    val chatlineE = messageEvent.map(msg => Chatline(nameS.value, msg))

    // The `Dotted` provides a context within a form of logical time is available.
    // ReplicatedList makes use of that to make sure that multiple replicas never enter an inconsistent state
    val history = Storing.storedAs("chathistory", Dotted(ReplicatedList.empty[Chatline])) {
      init =>
        Fold(init) {
          // We need to `merge` the current value with the “change” that is produced by the prepend operation.
          // I did not manage to explain the mechanism during the exercise session unfortunately,
          // but roughly the idea is that ReplicatedList is designed in a way that any ”modifying operations”
          // only return a description of teh change, not a full new state.
          // Merging then gets us to the full state which the fold stores.
          chatlineE branch { chatline => current `merge` current.mod(_.prepend(chatline)) }
        }
    }

    // TODO replicate

    val messages = history.map { chatlines =>
      chatlines.data.toList.map(chatline => p(s"${chatline.name}: ${chatline.message}").render)
    }

    section(
      nameData,
      messageData
    ).render.reattach(messages)
  }

}
