package com.dalas

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.streaming.Trigger
import java.nio.file.{Files, Paths}
import org.apache.spark.sql.functions.{col, sum}
//import org.apache.spark.sql.avro.functions.from_avro
import za.co.absa.abris.config.{AbrisConfig, FromAvroConfig}
import za.co.absa.abris.avro.functions.from_avro
import java.util.Properties
object sparkStreaming {
  def main(args: Array[String]): Unit = {
    val KAFKA_TOPIC_NAME_CONS = "fuel-iot-avro-test"
    val KAFKA_BOOTSTRAP_SERVERS_CONS = "broker1:29092,broker2:29093,broker3:29094"
    val CP_SCHEMA_REGISTRY_URL_CONS = "http://schema-registry:8081"

    // PostgreSQL Database Server Details
    val postgresql_host_name = "postgres-server"
    val postgresql_port_no = "5432"
    val postgresql_user_name = "demouser"
    val postgresql_password = "demouser"
    val postgresql_database_name = "demodb"
    val postgresql_driver = "org.postgresql.Driver"
    val postgresql_table_name = "dashboard_event_transacs_detail_agg_tbl"
    val postgresql_jdbc_url = "jdbc:postgresql://" + postgresql_host_name + ":" + postgresql_port_no + "/" + postgresql_database_name

    val abrisConfig: FromAvroConfig = AbrisConfig
      .fromConfluentAvro
      .downloadReaderSchemaByLatestVersion
      .andTopicNameStrategy("fuel-iot-avro-test")
      .usingSchemaRegistry(CP_SCHEMA_REGISTRY_URL_CONS)

    println("Real-Time Streaming Data Pipeline started...")

    val spark = SparkSession.builder()
      .appName("Real-Time Streaming Data Pipeline")
      .getOrCreate()


    spark.sparkContext.setLogLevel("ERROR")

    // Server Status Event Message Data from Kafka
    val df = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", KAFKA_BOOTSTRAP_SERVERS_CONS)
      .option("subscribe", KAFKA_TOPIC_NAME_CONS)
      .option("startingOffsets", "earliest")
      .load()

    println("Printing Schema of event_message_detail_df: ")
    df.printSchema()


    val fuelTransDF = df.select(from_avro(col("value"), abrisConfig).as("data"))
      .select(col("data.*"))

    fuelTransDF.printSchema()

    val fuelGroupedDF = fuelTransDF.groupBy("id_usr", "pump_site_id", "prod_name")
      .agg(sum("qty").alias("total_gallons"))

    fuelGroupedDF.printSchema()

    // Create the Database properties
    val db_connection_properties = new Properties()
    db_connection_properties.put("user", postgresql_user_name)
    db_connection_properties.put("password", postgresql_password)
    db_connection_properties.put("url", postgresql_jdbc_url)
    db_connection_properties.put("driver", postgresql_driver)


    fuelGroupedDF.writeStream
      .trigger(Trigger.ProcessingTime("30 seconds"))
      .outputMode("update")
      .foreachBatch { (batchDF: DataFrame, batchId: Long) =>
        // Transform batchDF and write it to sink/target/persistent storage
        // Write data from spark dataframe to database/table
        batchDF.write.mode("append").jdbc(postgresql_jdbc_url, postgresql_table_name, db_connection_properties)
      }
      .start()
      .awaitTermination()

     // Write result dataframe into console for debugging purpose
//        fuelTransDF.writeStream
//          .format("console")
//          .outputMode("append")
//          .option("truncate", false)
//          .start()
//          .awaitTermination()
  }
}
