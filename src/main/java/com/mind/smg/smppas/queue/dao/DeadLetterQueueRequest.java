package com.mind.smg.smppas.queue.dao;

import java.sql.Blob;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.Data;

@Data
@Entity
public class DeadLetterQueueRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	private String queueName;
	
	private Date dlconsumeTime;
	
	private Blob requestObject;
	
	private String consumerTag;
	
	private String correlationId;
	
	private Date requestReceiveDL;
	
	
}
