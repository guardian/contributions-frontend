import play.sbt.PlayRunHook
import sbt._
import java.net.InetSocketAddress

object AssetsWatch {

  var gruntWatch: Option[Process] = None
  var webpackWatch: Option[Process] = None

  def killall(): Unit = {
    gruntWatch.foreach(_.destroy())
    gruntWatch = None
    webpackWatch.foreach(_.destroy())
    webpackWatch = None
  }

  def apply(base: File): PlayRunHook = new PlayRunHook {

    override def afterStarted(addr: InetSocketAddress): Unit = {
      killall()
      gruntWatch = Some(Process("npm run watchGrunt", base).run)
      webpackWatch = Some(Process("npm run watchWebpack", base).run)
    }

    override def afterStopped(): Unit = killall()
  }
}
