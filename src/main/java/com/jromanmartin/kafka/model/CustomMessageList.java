package com.jromanmartin.kafka.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CustomMessageList implements Serializable {

    private static final long serialVersionUID = 138669946178014325L;
    
    @Schema(description = "List of messages", required = false)
	private List<CustomMessage> list = new ArrayList<>();

    /**
     * @return the customMessages
     */
    public List<CustomMessage> getCustomMessages() {
        return list;
    }

    /**
     * @param customMessages the customMessages to set
     */
    public void setCustomMessages(List<CustomMessage> customMessages) {
        this.list = customMessages;
    }

    public void addCustomMessage(CustomMessage customMessage) {
        if (null == list) {
            list = new ArrayList<CustomMessage>();
        }
        list.add(customMessage);
    }

}
