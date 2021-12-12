package com.corona.dataAccess.kafka

import org.apache.kafka.clients.producer.{Callback, KafkaProducer, ProducerRecord, RecordMetadata}
import org.slf4j.{Logger, LoggerFactory}

import java.net.InetAddress
import java.util.Properties
import java.util.concurrent.Future

object CustomKafkaClient {

  val log: Logger = LoggerFactory.getLogger("kafka client")
  private val kafkaProducer: KafkaProducer[String, String] = {
    val config = new Properties
    config.put("client.id", InetAddress.getLocalHost.getHostName)
    config.put("bootstrap.servers", "localhost:9092")
    config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    config.put("max.request.size","25925346")
   // config.put("acks", "all")
    new KafkaProducer[String,String](config)
  }

  def sendData(topic:String, key:String, value:String): Future[RecordMetadata] ={
    kafkaProducer.send(new ProducerRecord[String,String](topic,key,value), (metadata: RecordMetadata, exception: Exception) => {
      if (Option(exception).isDefined) {
        log.error(s"kafka push failed for country ${key}", exception)
      } else {
        log.info(s"kafka push successful. The offset number is ${metadata.offset().toString}")
      }
    })
  }

}
