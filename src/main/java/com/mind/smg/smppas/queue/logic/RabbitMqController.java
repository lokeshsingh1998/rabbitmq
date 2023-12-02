package com.mind.smg.smppas.queue.logic;


import java.io.Serializable;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mind.smg.smppas.queue.dao.QueueDetailsDto;
import com.mind.smg.smppas.queue.dao.RequestDtoDto;
import com.mind.smg.smppas.queue.dao.RequestQueue;
import com.mind.smg.smppas.queue.dao.UnitDto;


@RestController
@RequestMapping(value = "/rabbitmq")
public class RabbitMqController {

	
	  @Autowired
	  RabitMqService rabitMqService;
	  
	    @PostMapping(value = "/sender")
	    public void producer(@RequestBody UnitDto unitDto) throws Exception {
	    rabitMqService.send(unitDto);
	 
	    }
	
	    @GetMapping("/pauseQueue")
	    public String pauseQueue(@RequestParam String action,String queueName) {
	    	
	    return rabitMqService.pauseQueue(action,queueName);
	    }
	    
	    @GetMapping("/addConsumers")
	    public String addConsumers(@RequestParam Integer consumers,String queueName) {
	    	
	    	return rabitMqService.addConsumers(consumers,queueName);
	    }
	    
	    @GetMapping("/addPrefetchCount")
	    public String addPrefetchCount(@RequestParam Integer preFetchcount,String queueName) {
	    	
	    	return rabitMqService.addPrefetchCount(preFetchcount,queueName);
	    }
	    
	    @GetMapping("/getQueueDetails")
	    public QueueDetailsDto getMessageCount(@RequestParam String queueName) {
	    	
	    	return rabitMqService.getMessageCount(queueName);
	    }
	    
	    @GetMapping("/getAllQueues")
	    public List<QueueDetailsDto> getAllQueues(){
	    	
	    	return rabitMqService.getAllQueues();
	    }

	    @GetMapping("/getRequestByStatus")
	    public List<RequestDtoDto> getRequestByStatus(String status) {
	    	
	    	return rabitMqService.getRequestByStatus(status);
	    	
	    }
	    
	    @GetMapping("/deleteConsumers")
	    public String deleteConsumers(@RequestParam Integer consumers,String queueName) {
	    	
	    	return rabitMqService.deleteConsumers(consumers,queueName);
	    }
}
