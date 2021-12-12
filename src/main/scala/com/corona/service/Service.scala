package com.corona.service


import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.unmarshalling.Unmarshaller
import com.corona.dataAccess.kafka.CustomKafkaClient
import com.corona.dataAccess.rest.RestClient
import com.corona.model.{ClientProperties, WorldDataModel}
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}


class Service {
  val log: Logger = LoggerFactory.getLogger("service")
  val resource: Config = ConfigFactory.load("application.conf").getConfig("com.corona")
  implicit val actorSystem: ActorSystem[Nothing] = RestClient.actorSystem
  implicit val executionContext: ExecutionContextExecutor = actorSystem.executionContext
  /**
   * Gets corona virus history data from apify URL=> https://apify.com/covid-19
   * @return return map of country name and scala futures of HttpResponse
   */
  def getDataBlocking(countriesUrl:Option[(String,String)]=None):mutable.Map[String,String]={

    val data:mutable.Map[String,String] = mutable.Map()
    if(countriesUrl.isDefined){
          val countriesUrlTuple = countriesUrl.get
          log.info(s"getting corona virus data for ${countriesUrlTuple._1} from url ${countriesUrlTuple._2}")
          RestClient.getApiResponseBlocking(countriesUrlTuple._2) match {
            case Success(response) => data += countriesUrlTuple._1 -> response.body()
            case Failure(exception) => log.error(s"getting corona virus history data failed for ${countriesUrlTuple._1}",exception)
          }
      data
    }else{
      val countries = resource.getConfig("countries")
      countries.entrySet.forEach({
        entry => {
          log.info(s"getting corona virus data for ${entry.getKey} from url ${entry.getValue.unwrapped}")
          RestClient.getApiResponseBlocking(entry.getValue.unwrapped().toString) match {
            case Success(response) => data += entry.getKey -> response.body()
            case Failure(exception) => log.error(s"getting corona virus history data failed for ${entry.getKey}",exception)
          }
        }
      })
      data
    }

  }

  /**
   * Gets corona virus live data from apify URL=> https://apify.com/covid-19
   * The data is refreshed every 1 hour.
   * @return return map of country name and scala futures of HttpResponse
   */
  def getDataReactive(countries:mutable.Map[String,String]): mutable.Map[String, Future[HttpResponse]] ={

    val liveData:mutable.Map[String,Future[model.HttpResponse]] = mutable.Map()
    countries.foreach({
      entry => {
        log.info(s"getting corona virus live data for ${entry._1} from url ${entry._2}")
        liveData += entry._1 -> RestClient.getApiResponseReactiveAkka(entry._2.trim)
      }
    })
    liveData
  }

  /**
   *  Sends data to kafka.
   * @param futuresMap =  Needs map of country name and HttpResponse future.
   */
  def sendFuturesTOKafka(futuresMap:mutable.Map[String, Future[HttpResponse]]): Future[List[HttpResponse]] ={

    for((country,future)<- futuresMap){
      future.onComplete{
        case Success(response) =>
          val res = response.entity.toStrict(10*1000.seconds)
          res.onComplete{
            case Success(response) =>
              Unmarshaller.stringUnmarshaller(response.data) onComplete {
                case Success(data)=> CustomKafkaClient.sendData("coronavirus-live",country,data)
                case Failure(exception) => log error(s"marshalling of data failed for country $country", exception)
              }
            case Failure(exception) => log.error(s"getting data for country failed $country ",exception)
          }
        case Failure(exception) => log.error(s"getting data for country failed $country ",exception)
      }
    }
    val listFutures = futuresMap.view.map{case (_, value) => value}.toList
    Future.sequence(listFutures)
  }

  def sendDataToKafka(topic:String, historyURLMap:mutable.Map[String,String]): Unit ={

    for((country,data)<-historyURLMap){
     //blocking
      try{
        if(!country.equalsIgnoreCase("russia")){
          val historyData = getDataBlocking(Option((country,data)))
          CustomKafkaClient.sendData(topic,country,historyData.values.head).get()
        }else{
          log.warn("skipping getting history data for russia")
        }

      }catch {
        case exception: Exception => log.error(s"couldn't get history data for country $country. exception occurred")
      }

    }

  }

  def extractDataFromJson(data:String, property:String):mutable.Map[String,String]={
    import net.liftweb.json._
    implicit val formats: DefaultFormats.type = DefaultFormats
    val addressMap: mutable.Map[String, String] = mutable.Map()
    val json =  parse(data)
    val countries = json.extract[List[WorldDataModel]]
    if(property==ClientProperties.LIVE_DATA){
      countries.foreach{
        x=> addressMap += x.country -> x.moreData
      }
    }else{
      countries.foreach{
        x=> addressMap += x.country -> x.historyData
      }
    }
    addressMap
  }

}
