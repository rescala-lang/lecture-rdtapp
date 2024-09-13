package rdtapp

import org.scalajs.dom.UIEvent
import org.scalajs.dom.html.{Button, Div}
import rdtapp.helpers.RenderUtil
import rdts.base.{Lattice, LocalUid}
import rdts.syntax.DeltaBuffer
import reactives.extra.Tags.reattach
import reactives.operator.Event.CBR
import reactives.operator.{Event, Fold, FoldState, Signal}
import scalatags.JsDom.all.*
import todo.AppDataManager

object MainUI {

  /** A `given` allows methods to find this by it’s type if it is in scope. */
  given replicaId: LocalUid = LocalUid.gen()

  /** helper function to remove some boilerplate in the Fold below */
  extension [T](event: Event[T])
    def deltaBranch[S: Lattice](f: FoldState[S] ?=> T => S): Fold.Branch[DeltaBuffer[S]] = {
      event.branch { v => Fold.current.mod(app => f(using FoldState(app))(v)) }
    }

  /** This resets the Delta buffer in the fold below, to not contain any deltas */
  def resetBuffer[T] = Fold.Branch[DeltaBuffer[T]](Nil, isStatic = false, _ => Fold.current.clearDeltas())

  def makeInputEvent(placeholderText: String) = RenderUtil.inputFieldHandler(
    input(placeholder := placeholderText),
    oninput,
    clear = false
  )

  def makeButtonEvent(description: String): CBR[UIEvent, Button] =
    Event.fromCallback[org.scalajs.dom.html.Button, UIEvent](
      button(description, onclick := Event.handle).render
    )

  def getContents(): Div = {

    val downvoteButton  = makeButtonEvent(Character.toString(0x1f44e))
    val messageHandling = makeInputEvent("<your message to the world>")

    val stateSignal: Signal[DeltaBuffer[MiniSocial]] = {

      AppDataManager.hookup(MiniSocial()) { (init, incoming) =>
        Fold(init)(
          resetBuffer,

          messageHandling.event.deltaBranch { inputText =>
            Fold.current.setMessage(inputText)
          },
          downvoteButton.event.deltaBranch { _ =>
            Fold.current.dislike()
          },
          incoming
        )
      }
    }

    val appStateSignal = Signal { stateSignal.value.state }
    val messageSignal = Signal {
      span(appStateSignal.value.message.value).render
    }
    val downvotesSignal = Signal {
      span(appStateSignal.value.downvotes.value).render
    }

    /* Just a DSL to create some HTML */
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
          td.render.reattach(downvotesSignal),
          td(downvoteButton.data)
        )
      ),
      p(messageHandling.data)
    ).render

  }

}
