package com.recommendmecheap.scraper;

import java.util.Properties;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.errors.RecordTooLargeException;
import org.apache.kafka.common.requests.ProduceRequest;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

public class ScraperProducer {
	public final static String TOPIC = "scrapeddata";
	public final static String BOOTSTRAP_SERVERS = "localhost:9092";
	
	public static Producer<Long, String> createProducer()
	{
		Properties props = new Properties();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
																				BOOTSTRAP_SERVERS);
		props.put(ProducerConfig.CLIENT_ID_CONFIG, "KafkaExampleProducer");
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
																		LongSerializer.class.getName());
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
																StringSerializer.class.getName());
		return new KafkaProducer<>(props);
	}
}