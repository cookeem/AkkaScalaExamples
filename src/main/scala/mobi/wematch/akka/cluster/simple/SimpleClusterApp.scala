package mobi.wematch.akka.cluster.simple

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object SimpleClusterApp {
  def main(args: Array[String]): Unit = {
    if (args.isEmpty)
      startup(Seq("2551", "2552", "0"))
    else
      startup(args)
  }

  def startup(ports: Seq[String]): Unit = {
    val clusters = ports.map{ port =>
      // Override the configuration of the port
      val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
        withFallback(ConfigFactory.load("cluster/application"))

      // Create an Akka system
      val system = ActorSystem("ClusterSystem", config)
      // Create an actor that handles cluster domain events
      val clusterListener = system.actorOf(Props[SimpleClusterListener2], name = "clusterListener")
      Tuple2(system,clusterListener)
    }
    println("###########################################################")
    println("Sleep for 10 seconds")
    println("###########################################################")
    Thread.sleep(10000)
    println("###########################################################")
    println("Sleep finish")
    println("###########################################################")
    clusters.foreach(t => {
      val system = t._1
      val clusterListener = t._2
      clusterListener ! "stop"
      system.shutdown()
    })
  }
}

