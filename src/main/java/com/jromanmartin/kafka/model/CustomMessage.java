package com.jromanmartin.kafka.model;

import java.io.Serializable;

import io.swagger.annotations.ApiModelProperty;

public class CustomMessage implements Serializable {

	private static final long serialVersionUID = 1305278483346223763L;

	@ApiModelProperty(notes = "Key to identify this message", position = 0, required = false)
	private Long key;

	@ApiModelProperty(notes = "Timestamp", position = 1, readOnly = true)
	private String timestamp;
	
	@ApiModelProperty(notes = "Content", position = 2, required = true)
	private String content;

	@ApiModelProperty(notes = "Partition number", position = 3, required = false, readOnly = true)
	private int partition;

	@ApiModelProperty(notes = "Offset in the partition", position = 4, required = false, readOnly = true)
	private long offset;
	
	public Long getKey() {
		return key;
	}

	public void setKey(Long key) {
		this.key = key;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	/**
	 * @return the partition
	 */
	public int getPartition() {
		return partition;
	}

	/**
	 * @param partition the partition to set
	 */
	public void setPartition(int partition) {
		this.partition = partition;
	}

	/**
	 * @return the offset
	 */
	public long getOffset() {
		return offset;
	}

	/**
	 * @param offset the offset to set
	 */
	public void setOffset(long offset) {
		this.offset = offset;
	}

}
