package com.example.cloudflow

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import JsonCallRecord._
import cloudflow.akkastream.AkkaServerStreamlet
import cloudflow.akkastream.util.scaladsl.HttpServerLogic
import cloudflow.streamlets.StreamletShape
import cloudflow.streamlets.avro.AvroOutlet
import com.example.data.SyncMailbox

class SyncEventIngress extends AkkaServerStreamlet {

  val out = AvroOutlet[SyncMailbox]("out")

  final override val shape       = StreamletShape.withOutlets(out)
  final override def createLogic = HttpServerLogic.default(this, out)
}
