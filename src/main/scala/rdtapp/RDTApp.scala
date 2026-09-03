package rdtapp
import org.scalajs.dom
import org.scalajs.dom.document
import rdts.base.Lattice
import reactives.extra.Tags.*
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

    // val webrtc = WebRTCConnectionView(AppDataManager.dataManager).example()

    // document.body.appendChild(webrtc.render)

    val appResult = AppDataManager.receivedCallback.map { _ =>
      val state = AppDataManager.dataManager.allPayloads.map(_.data).reduceOption(Lattice.merge)
      all.div(
        all.pre(all.stringFrag(pprint.apply(state).plainText)),
        all.br(),
        all.pre(all.stringFrag(pprint.apply(AppDataManager.dataManager.replicaId).plainText))
      ).render
    }.hold(all.span.render)

    document.body.appendChild(
      all.div.render.reattach(appResult)
    )

    timer.scheduleAtFixedRate(
      () =>
        try
            AppDataManager.dataManager.tick()
        catch
            case any => println(s"request failed: $any")
      ,
      1000,
      1000
    )

    ()
  }

}
