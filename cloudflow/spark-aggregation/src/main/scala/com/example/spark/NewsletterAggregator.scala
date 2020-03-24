package com.example.spark

import cloudflow.spark.sql.SQLImplicits._
import cloudflow.spark.{SparkStreamlet, SparkStreamletLogic}
import cloudflow.streamlets.avro.{AvroInlet, AvroOutlet}
import cloudflow.streamlets.{DurationConfigParameter, StreamletShape}
import com.example.data.Newsletter
import com.example.data.AggregatedNewsletterStats
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.functions.{avg, sum, window}
import org.apache.spark.sql.streaming.OutputMode
import org.apache.spark.sql.types.{LongType, TimestampType}

class NewsletterAggregator extends SparkStreamlet {

  val in = AvroInlet[Newsletter]("in")
  val out = AvroOutlet[AggregatedNewsletterStats]("out")

  override def shape() = StreamletShape(in, out)

  override def createLogic = new SparkStreamletLogic {
    val watermark     = context.streamletConfig.getDuration(Watermark.key)
    val groupByWindow = context.streamletConfig.getDuration(GroupByWindow.key)

    //tag::docs-aggregationQuery-example[]
    override def buildStreamingQueries = {
      val dataset   = readStream(in)
      val outStream = process(dataset)
      writeStream(outStream, out, OutputMode.Update).toQueryExecution
    }

    private def process(inDataset: Dataset[Newsletter]): Dataset[AggregatedNewsletterStats] = {
      val query =
        inDataset
          .withColumn("ts", $"timestamp".cast(TimestampType))
          .withWatermark("ts", s"${watermark.toMillis()} milliseconds")
          .groupBy(window($"ts", s"${groupByWindow.toMillis()} milliseconds"))
          .agg(avg($"duration").as("avgCallDuration"), sum($"duration").as("totalCallDuration"))
          .withColumn("windowDuration", $"window.end".cast(LongType) - $"window.start".cast(LongType))

      query
        .select($"window.start".cast(LongType).as("startTime"), $"windowDuration", $"avgCallDuration", $"totalCallDuration")
        .as[AggregatedNewsletterStats]
    }
    //end::docs-aggregationQuery-example[]
  }

  val GroupByWindow = DurationConfigParameter("group-by-window", "Window duration for the moving average computation", Some("1 minute"))

  val Watermark = DurationConfigParameter("watermark", "Late events watermark duration: how long to wait for late events", Some("1 minute"))

  override def configParameters = Vector(GroupByWindow, Watermark)
}
