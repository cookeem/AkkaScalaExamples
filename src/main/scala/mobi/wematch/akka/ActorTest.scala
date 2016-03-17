package mobi.wematch.akka

import akka.actor.{ActorSystem, Props, ActorRef, Actor}

/**
 * Created by cookeem on 15/10/16.
 */
object ActorTest extends App {
  case class Greeting(message: String)

  class Ha extends Actor {
    def receive = {
      case "heng" =>
        i = i + 1
        if (i < 10) {
          printer ! Greeting("哼"+","+i)
          java.lang.Thread.sleep(1000)
          sender ! "ha"
        }
      case _ => println("ha what?")
    }
  }

  class Heng(ha: ActorRef) extends Actor {
    def receive = {
      case "start" => ha ! "heng"
      case "ha" =>
        i = i + 1
        if (i < 10) {
          java.lang.Thread.sleep(1000)
          printer ! Greeting("哈"+","+i)
          ha ! "heng"
        } else {
          system.shutdown
        }
      case _ => println("heng what?")
    }
  }

  class GreetPrinter extends Actor {
    def receive = {
      case Greeting(message) => println(message)
    }
  }

  val system = ActorSystem("HengHaSystem")
  val ha = system.actorOf(Props[Ha], name = "ha")
  val heng = system.actorOf(Props(new Heng(ha)), name = "heng")
  val printer = system.actorOf(Props[GreetPrinter], name = "printer")
  var i = 0
  heng ! "start"

}


