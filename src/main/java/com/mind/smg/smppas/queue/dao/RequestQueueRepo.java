package com.mind.smg.smppas.queue.dao;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RequestQueueRepo extends JpaRepository<RequestQueue, Long>{

	RequestQueue findByCorrelationId(String correlationId);
	
	@Query("select distinct new com.mind.smg.smppas.queue.dao.RequestDtoDto(r.id,r.queueName,r.consumerTag,r.correlationId, r.status,"
			+ "	r.requestRecieveTime, r.requestConsumeTime,r.inProcessStartTime,r.deadLetterQueueId,r.unitObject) from RequestQueue r where r.status=:status")	
	List<RequestDtoDto> findByStatus(String status);

	@Query("select distinct new com.mind.smg.smppas.queue.dao.RequestDtoDto(r.id,r.queueName,r.consumerTag,r.correlationId, r.status,"
			+ "	r.requestRecieveTime, r.requestConsumeTime,r.inProcessStartTime,r.deadLetterQueueId,r.unitObject) from RequestQueue r")
	List<RequestDtoDto> findAllList();

}
