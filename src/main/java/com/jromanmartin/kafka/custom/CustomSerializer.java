package com.jromanmartin.kafka.custom;

import java.util.Map;

import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jromanmartin.kafka.model.CustomMessage;

public class CustomSerializer implements Serializer<CustomMessage> {

	private final static Logger LOGGER = LoggerFactory.getLogger(CustomSerializer.class);
	
	@Override
	public void configure(Map<String, ?> configs, boolean isKey) {
		LOGGER.debug("CustomSerializer configured. ConfigMap: {}", configs);
	}

	@Override
	public byte[] serialize(String topic, CustomMessage data) {
		byte[] retVal = null;
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			retVal = objectMapper.writeValueAsString(data).getBytes();
		} catch (Exception exception) {
			System.out.println("Error in serializing object" + data);
		}
		return retVal;
	}

	@Override
	public void close() {
		LOGGER.debug("CustomSerializer closed");
	}

}