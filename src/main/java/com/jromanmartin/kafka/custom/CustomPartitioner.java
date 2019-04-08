package com.jromanmartin.kafka.custom;

import java.util.Map;
import java.util.Random;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomPartitioner implements Partitioner {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(CustomPartitioner.class);

	private static final int PARTITION_COUNT = 3;

	@Override
	public void configure(Map<String, ?> configs) {
		LOGGER.debug("CustomPartitioner configured. ConfigMap: {}", configs);
	}

	@Override
	public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
		if (null == key) {
			int keyInt = (new Random()).nextInt(PARTITION_COUNT);
			return keyInt % PARTITION_COUNT;
		}
		
		int keyInt = Integer.parseInt(key.toString());
		
		if (keyInt > PARTITION_COUNT) {
			return keyInt % PARTITION_COUNT;
		}
		
		return keyInt;
	}

	@Override
	public void close() {
		LOGGER.debug("CustomPartitioner closed");
	}

}
