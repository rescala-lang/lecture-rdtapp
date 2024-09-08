package rdtapp

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.{CodecMakerConfig, JsonCodecMaker}
import org.scalajs.dom.html.Button
import org.scalajs.dom.{UIEvent, document}
import rdtapp.helpers.RenderUtil
import rdts.base.{Bottom, Lattice, LocalUid}
import rdts.datatypes.LastWriterWins
import rdts.syntax.DeltaBuffer
import reactives.extra.Tags.reattach
import reactives.operator.Event.CBR
import reactives.operator.{Event, Fold, FoldState, Signal}
import scalatags.JsDom.all.*
import todo.AppDataManager

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

case class ApplicationState(
    message: LastWriterWins[String] = LastWriterWins.fallback(""),
    upvotes: GrowOnlyCounter = GrowOnlyCounter.zero,
    downvotes: GrowOnlyCounter = GrowOnlyCounter.zero
)

object ApplicationState {
  given lattice: Lattice[ApplicationState] = Lattice.derived

  given bottom: Bottom[ApplicationState] = Bottom.provide(ApplicationState())

  given codec: JsonValueCodec[ApplicationState] = JsonCodecMaker.make(CodecMakerConfig.withMapAsArray(true))

}

@JSExportTopLevel("RDTApp")
object RDTApp {

  given replicaId: LocalUid = LocalUid.gen()

  @JSExport("start")
  def start(): Unit = {
    val content = getContents()

    document.body.replaceChild(content, document.body.firstChild)

    document.body.appendChild(p(style := "height: 3em").render)
  }

  extension [T](event: Event[T])
    def deltaBranch[S: Lattice](f: FoldState[S] ?=> T => S): Fold.Branch[DeltaBuffer[S]] = {
      event.branch { v => Fold.current.mod(app => f(using FoldState(app))(v)) }
    }

  def getContents() = {

    def makeInputEvent(placeholderText: String) = RenderUtil.inputFieldHandler(
      input(placeholder := placeholderText),
      oninput,
      clear = false
    )

    def makeButtonEvent(description: String): CBR[UIEvent, Button] =
      Event.fromCallback[org.scalajs.dom.html.Button, UIEvent](
        button(description, onclick := Event.handle).render
      )

    val messageHandling = makeInputEvent("<your message to the world>")
    val upvoteButton    = makeButtonEvent(Character.toString(0x1f44d))
    val downvoteButton  = makeButtonEvent(Character.toString(0x1f44e))

    val replicatedState = {
      val init = ApplicationState()
      AppDataManager.hookup(init, identity, Some.apply) { (init, incoming) =>
        Fold(init)(
          messageHandling.event.deltaBranch { inputText =>
            val messageDelta = Fold.current.message.write(inputText)
            ApplicationState(message = messageDelta)
          },
          upvoteButton.event.deltaBranch { _ =>
            val upvoteDelta = Fold.current.upvotes.add(1)
            ApplicationState(upvotes = upvoteDelta)
          },
          downvoteButton.event.deltaBranch { _ =>
            val downVoteDelta = Fold.current.downvotes.add(1)
            ApplicationState(downvotes = downVoteDelta)
          },
          incoming
        )
      }
    }

    val appStateSignal = Signal { replicatedState.value.state }
    val messageSignal = Signal {
      span(appStateSignal.value.message.value).render
    }
    val upvotesSignal = Signal {
      span(appStateSignal.value.upvotes.value).render
    }
    val downvotesSignal = Signal {
      span(appStateSignal.value.downvotes.value).render
    }

    div(
      table(
        thead(
          th("message to vote on"),
          th("~~~~~"),
          th(""),
          th("upvotes"),
          th(""),
          th("downvotes"),
        ),
        tr(
          td.render.reattach(messageSignal),
          td(),
          td.render.reattach(upvotesSignal),
          td(upvoteButton.data),
          td.render.reattach(downvotesSignal),
          td(downvoteButton.data)
        )
      ),
      p(messageHandling.data)
    ).render

  }

}
