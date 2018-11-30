package com.jromanmartin.kafka.custom;

import java.util.Map;

import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jromanmartin.kafka.model.CustomMessage;

public class CustomDeserializer implements Deserializer<CustomMessage> {

	private final static Logger LOGGER = LoggerFactory.getLogger(CustomDeserializer.class);
	
	@Override
	public void configure(Map<String, ?> configs, boolean isKey) {
		LOGGER.debug("CustomDeserializer configured. ConfigMap: {}. isKey: {}", configs, isKey);
	}

	@Override
	public CustomMessage deserialize(String topic, byte[] data) {
		ObjectMapper mapper = new ObjectMapper();
		CustomMessage object = null;
		try {
			object = mapper.readValue(data, CustomMessage.class);
		} catch (Exception exception) {
			System.out.println("Error in deserializing bytes " + exception);
		}
		return object;
	}

	@Override
	public void close() {
		LOGGER.debug("CustomDeserializer closed");
	}
}