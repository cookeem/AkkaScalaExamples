package mobi.wematch.akka

import akka.actor._
import akka.event.Logging
import akka.util.Timeout
import akka.pattern.ask

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Created by cookeem on 15/10/19.
 */
object ActorTest2 extends App {
  val system = ActorSystem("DemoSystem")

  //Ask模式返回数据
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
  implicit val timeout = Timeout(5 seconds)
  val printer = system.actorOf(Props[GreetPrinter], name = "printer")
  val future = printer ? "hello"
  val result = Await.result(future, timeout.duration).asInstanceOf[String]
  println("Await for the result is:"+result)

  printer ! "timeout"
  printer ! "log"
  printer ! "close"

  //become + unbecome
  case object Swap
  class Swapper extends Actor {
    import context._
    val log = Logging(context.system, this)
    def receive = {
      case Swap =>
        log.info("Hi")
        become({
          case Swap =>
            log.info("Ho")
            unbecome() // resets the latest 'become' (just for fun)
        }, discardOld = false) // push on top instead of replace
      case "close" => context.stop(self); log.info("Swap context stopp!")
    }
  }

  val swapper = system.actorOf(Props(new Swapper), name = "swapper")
  swapper ! Swap
  swapper ! Swap
  swapper ! Swap
  swapper ! Swap

  //Actor继承
  case object GiveMeThings
  case class Give(thing: Any)
  trait ProducerBehavior {
    this: Actor =>
      val producerBehavior: Receive = {
        case GiveMeThings =>
          sender() ! Give("thing")
      }
  }
  trait ConsumerBehavior {
    this: Actor with ActorLogging =>
      val consumerBehavior: Receive = {
        case ref: ActorRef =>
          ref ! GiveMeThings
        case Give(thing) =>
          log.info("Got a thing! It's {}", thing)
      }
  }
  class Producer extends Actor with ProducerBehavior {
    def receive = producerBehavior
  }
  class Consumer extends Actor with ActorLogging with ConsumerBehavior {
    def receive = consumerBehavior
  }
  class ProducerConsumer extends Actor with ActorLogging with ProducerBehavior with ConsumerBehavior {
    def receive = producerBehavior orElse consumerBehavior
  }
  val pc = system.actorOf(Props(new ProducerConsumer), name = "pc")
  pc ! GiveMeThings

  system.shutdown()
}
