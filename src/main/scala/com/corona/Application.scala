package com.corona

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import com.corona.model.ClientProperties
import com.corona.service.Service
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContextExecutor

object Application {

  implicit val actorSystem: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "coronavirus")
  implicit val executionContext: ExecutionContextExecutor = actorSystem.executionContext
  val log: Logger = LoggerFactory.getLogger("Application")
  def main(args: Array[String]): Unit = {

    val service: Service = new Service
    if (args.length !=1){
      log.error("please give whether you want history data or live data as argument")
      actorSystem.terminate()
      System.exit(0)
    }
    if (args(0) == "history") {
      val countriesUrlJSON = service.getDataBlocking()
      val countryUrlMap = service.extractDataFromJson(countriesUrlJSON.values.head,ClientProperties.HISTORY_DATA)
      service.sendDataToKafka(ClientProperties.HISTORY_TOPIC, countryUrlMap)
      actorSystem.terminate()
      System.exit(0)
    } else {
      val data = service.getDataBlocking()
      val countryUrlMap = service.extractDataFromJson(data.values.head,ClientProperties.LIVE_DATA)
      val future = service.sendFuturesTOKafka(service.getDataReactive(countryUrlMap))
      future.onComplete { _ =>
        Thread.sleep(5 * 1000)
        actorSystem.terminate()
        System.exit(0)
      }
    }
  }
}
