package com.mind.smg.smppas.queue.dao;

import java.io.Serializable;
import java.sql.Blob;
import java.util.Date;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.Data;

@Data
public class RequestDtoDto implements Serializable {

	
	private Long id;
	
	private String queueName;
	
	private String consumerTag;
	
	private String correlationId;
	
	private Blob unitObject;
	
	private String status;
	
	private Date requestRecieveTime;
	
	private Date requestConsumeTime;
	
	private Date inProcessStartTime;
	
	private Long deadLetterQueueId;
	
	private Long userId;
	
	private UnitDto unitDto;

	public RequestDtoDto(Long id, String queueName, String consumerTag, String correlationId, String status,
			Date requestRecieveTime, Date requestConsumeTime, Date inProcessStartTime, Long deadLetterQueueId,Blob unitObject) {
		super();
		this.id = id;
		this.queueName = queueName;
		this.consumerTag = consumerTag;
		this.correlationId = correlationId;
		this.status = status;
		this.requestRecieveTime = requestRecieveTime;
		this.requestConsumeTime = requestConsumeTime;
		this.inProcessStartTime = inProcessStartTime;
		this.deadLetterQueueId = deadLetterQueueId;
		this.unitObject=unitObject;
	}

	public RequestDtoDto() {
		super();
	}
	
	
	
	
	
}
