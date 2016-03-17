package mobi.wematch.akka

/**
 * Created by cookeem on 15/10/19.
 */

import akka.actor.{ActorSystem, Terminated, Props, Actor}
import akka.event.Logging
import akka.routing.ActorRefRoutee
import akka.routing.Router
import akka.routing.RoundRobinRoutingLogic

import scala.concurrent.duration._

object ActorRouteTest {
  val system = ActorSystem("DemoSystem")

  class GreetPrinter extends Actor {
    val log = Logging(context.system, this)
    var i = 0
    def receive = {
      case "hello" => sender ! "hi"
      case "log" => log.info("get log message")
      case "timeout" => {
        i = i + 1
        //每5秒没有收到消息就会抛出ReceiveTimeout异常
        if (i % 2 == 0) context.setReceiveTimeout(Duration.Undefined)
        else context.setReceiveTimeout(5 seconds)
      }
      case "close" => {
        //关闭ActorRef
        context.stop(self)
        log.info("GreetPrinter context stop !")
      }
      case message => println(message)
    }
  }

  class Master extends Actor {
    var router = {
      val routees = Vector.fill(5) {
        val r = context.actorOf(Props[GreetPrinter])
        context watch r
        ActorRefRoutee(r)
      }
      Router(RoundRobinRoutingLogic(), routees)
    }
    def receive = {
      case w: GreetPrinter =>
        router.route(w, sender())
      case Terminated(a) =>
        router = router.removeRoutee(a)
        val r = context.actorOf(Props[GreetPrinter])
        context watch r
        router = router.addRoutee(r)
    }
  }

  def main (args: Array[String]){
    val master = system.actorOf(Props[Master], name="master")
    master ! "log"
  }
}
