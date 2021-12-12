package com.corona.model

case class WorldDataModel(infected: String, tested: String, recovered: String, deceased: String, country: String,
  moreData: String, historyData: String, sourceUrl: Option[String]=None, lastUpdatedSource:Option[String]=None, lastUpdatedApify: String)
