import play.sbt.PlayRunHook
import sbt._
import java.net.InetSocketAddress

object AssetsWatch {
  def apply(base: File): PlayRunHook = new PlayRunHook {
    var gruntWatch: Option[Process] = None
    var webpackWatch: Option[Process] = None

    override def afterStarted(addr: InetSocketAddress): Unit = {
      gruntWatch = Some(Process("npm run watchGrunt", base).run)
      webpackWatch = Some(Process("npm run watchWebpack", base).run)
    }

    override def afterStopped(): Unit = {
      gruntWatch.foreach(_.destroy())
      gruntWatch = None
      webpackWatch.foreach(_.destroy())
      webpackWatch = None
    }
  }
}
