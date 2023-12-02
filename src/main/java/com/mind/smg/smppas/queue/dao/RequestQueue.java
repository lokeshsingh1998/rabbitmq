package com.mind.smg.smppas.queue.dao;

import java.io.Serializable;
import java.sql.Blob;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.Data;

@Data
@Entity
public class RequestQueue implements Serializable {

	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY )
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

	
	
}

