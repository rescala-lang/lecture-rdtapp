package rdtapp
import org.scalajs.dom
import org.scalajs.dom.document
import rdts.base.Lattice
import reactives.extra.Tags.*
import replication.WebRTCConnectionView
import scalatags.JsDom.all
import scalatags.JsDom.all.*
import todo.AppDataManager

import java.util.Timer
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("RDTApp")
object RDTApp {

  val timer = new Timer()

  @JSExport("start")
  def start(): Unit = {
    val content = MainUI.getContents()

    document.body.replaceChild(content, document.body.firstElementChild)

    document.body.appendChild(p(style := "height: 3em").render)

    val webrtc = WebRTCConnectionView(AppDataManager.dataManager).example()

    document.body.appendChild(webrtc.render)

    document.body.appendChild:
      all.div.render.reattach(AppDataManager.allCallback.hold((_: Any) => ()).map(_ =>
        val state = AppDataManager.dataManager.allDeltas.reduceOption(Lattice.merge)
        all.div(
          all.pre(all.stringFrag(pprint.apply(state.getOrElse(null)).plainText)),
          all.br(),
          all.pre(all.stringFrag(pprint.apply(AppDataManager.dataManager.selfContext).plainText))
        ).render
      ))

    timer.scheduleAtFixedRate(
      { () =>
        try
          AppDataManager.dataManager.requestData()
        catch
          case any => println(s"request failed: $any")
      },
      1000,
      1000
    )

    ()
  }

}
