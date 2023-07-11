package com.dalas.producer;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import com.dalas.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.Scanner;

public class GasStationsIotProducer {

    static final Logger log = LoggerFactory.getLogger(GasStationsIotProducer.class);
    private static final String TOPIC_NAME = "fuel-iot-avro-test";
    private static final String CSV_FILE_PATH = "producer/src/main/resources/transacciones_surtidor.csv";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static void main(String[] args) throws IOException {
        Properties kaProperties = new Properties();
        kaProperties.put("bootstrap.servers", "localhost:9092,localhost:9093,localhost:9094");
        kaProperties.put("key.serializer", "org.apache.kafka.common.serialization.LongSerializer");
        kaProperties.put("value.serializer", "io.confluent.kafka.serializers.KafkaAvroSerializer");
        kaProperties.put("schema.registry.url", "http://localhost:8081");

        try (Producer<Long, Transaction> producer = new KafkaProducer<>(kaProperties)) {

            FileReader fileReader = new FileReader(CSV_FILE_PATH);
            Scanner scanner = new Scanner(fileReader);

            // Skip the headers line
            scanner.nextLine();

            while (scanner.hasNextLine()) {

                String line = scanner.nextLine();
                String[] values = line.split(",");

                if (values.length != 32) {
                    continue;
                }

                // Parse the string into LocalDateTime
                LocalDateTime localDateTime1 = LocalDateTime.parse(values[15], formatter);
                LocalDateTime localDateTime2 = LocalDateTime.parse(values[16], formatter);

                // Convert LocalDateTime to Instant using UTC timezone
                Instant instant1 = localDateTime1.toInstant(ZoneOffset.UTC);
                Instant instant2 = localDateTime2.toInstant(ZoneOffset.UTC);

                // Extract the long timestamp
                long timestamp1 = instant1.toEpochMilli();
                long timestamp2 = instant2.toEpochMilli();

                Transaction transaction = new Transaction(
                        NumberUtils.toLong(values[0]),
                        NumberUtils.toInt(values[1]),
                        NumberUtils.toInt(values[2]),
                        NumberUtils.toInt(values[3]),
                        NumberUtils.toInt(values[4]),
                        values[5],
                        NumberUtils.toInt(values[6]),
                        NumberUtils.toInt(values[7]),
                        values[8],
                        NumberUtils.toDouble(values[9]),
                        NumberUtils.toInt(values[10]),
                        NumberUtils.toInt(values[11]),
                        values[12],
                        NumberUtils.toDouble(values[13]),
                        values[14],
                        instant1,
                        instant2,
                        values[17],
                        NumberUtils.toDouble(values[18]),
                        NumberUtils.toDouble(values[19]),
                        NumberUtils.toDouble(values[20]),
                        NumberUtils.toDouble(values[21]),
                        NumberUtils.toDouble(values[22]),
                        NumberUtils.toDouble(values[23]),
                        NumberUtils.toDouble(values[24]),
                        NumberUtils.toDouble(values[25]),
                        values[26],
                        NumberUtils.toDouble(values[27]),
                        NumberUtils.toDouble(values[28]),
                        NumberUtils.toInt(values[29]),
                        NumberUtils.toDouble(values[30]),
                        values[31]
                );
                ProducerRecord<Long, Transaction> producerRecord = new ProducerRecord<>(TOPIC_NAME, transaction.getUniqIdTrns(), transaction);
                producer.send(producerRecord);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {

                }

            }
            producer.close();
        }
    }
}
