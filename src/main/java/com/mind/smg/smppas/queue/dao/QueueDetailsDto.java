package com.mind.smg.smppas.queue.dao;

import lombok.Data;

@Data
public class QueueDetailsDto {

	
	private String queueName;
	
	private Integer messageCount;
	
	private Integer consumerCount;
	
}
