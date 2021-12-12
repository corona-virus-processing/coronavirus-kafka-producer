package com.corona.dataAccess.rest

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.{Http, model}
import akka.http.scaladsl.model.HttpMethods
import java.net.URI
import java.net.http.{HttpClient, HttpResponse}
import java.net.http.HttpResponse.BodyHandlers
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.Try

object RestClient {

  implicit val actorSystem: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "coronavirus")
  implicit val executionContext: ExecutionContextExecutor = actorSystem.executionContext

  /**
   * Uses java 11 httpClient
   * This method is blocking in nature
   * @param apiPath = url of the api from where the response is needed
   * @return returns http response enclosed with functional Try
   */
  def getApiResponseBlocking(apiPath:String):Try[HttpResponse[String]]={
    val client:HttpClient = HttpClient.newHttpClient()
    val request:java.net.http.HttpRequest = java.net.http.HttpRequest.newBuilder()
      .uri(URI.create(apiPath))
      .build()
    Try(client.send(request,BodyHandlers.ofString()))
  }

  /**
   * Akka Http reactive client. Needs Actors and executionContextExecutor defined implicitly
   * @param apiPath = url of the api from where the response is needed. The HTTP method is GET
   * @return returns scala futures http response.
   */
  def getApiResponseReactiveAkka(apiPath:String): Future[model.HttpResponse] ={
    val responseFuture: Future[model.HttpResponse] = Http()
      .singleRequest(model.HttpRequest(method = HttpMethods.GET, uri = apiPath))
    responseFuture
  }

}
