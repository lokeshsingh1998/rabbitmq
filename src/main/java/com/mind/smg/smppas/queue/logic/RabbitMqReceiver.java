package com.mind.smg.smppas.queue.logic;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.sql.rowset.serial.SerialException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.mind.smg.smppas.queue.dao.RequestQueue;
import com.mind.smg.smppas.queue.dao.RequestQueueRepo;
import com.mind.smg.smppas.queue.dao.UnitDto;
import com.rabbitmq.client.Channel;

@Component

public class RabbitMqReceiver {

	@Autowired
	RabitMqService rabitMqService;

	@Autowired
	RabbitTemplate rabbitTemplate;

	@Autowired
	RequestQueueRepo requestQueueRepo;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	private static Logger logger = LogManager.getLogger(RabbitMqReceiver.class.toString());

	// @RabbitHandler
	@RabbitListener(queues = "${rabbitmq.queue}", id = "${rabbitmq.listenerId}")
	public void receiver(Message message) throws Exception {

		Long requestQueueId = jdbcTemplate.queryForObject("select id from request_queue where correlation_id=?",
				Long.class, ((Message) message).getMessageProperties().getCorrelationId());

		Integer result = jdbcTemplate.update(
				"update request_queue set consumer_tag=?, queue_name=?,in_process_start_time=?,status=? where id=?",
				message.getMessageProperties().getConsumerTag(), message.getMessageProperties().getConsumerQueue(),
				new Date(), "INPROCESS", requestQueueId);

		if (result != 1) {
			throw new Exception();
		}

		generate();
//		

		byte[] body = ((Message) message).getBody();
		UnitDto unitDto = new Gson().fromJson(new String(body), UnitDto.class);

		if (unitDto.getUserId() < 1) {
			throw new Exception();
		}

		System.out.println("Unit Details=" + new String(body) + " with deleiveryId="
				+ ((Message) message).getMessageProperties().getDeliveryTag() + " and message id="
				+ ((Message) message).getMessageProperties().getMessageId() + " cosuming by the consumer= "
				+ ((Message) message).getMessageProperties().getConsumerTag());

		System.out.println(((Message) message).getMessageProperties().getMessageCount());

		Integer res = jdbcTemplate.update("update request_queue set status=?,request_consume_time=? where id=?",
				"COMPLETE", new Date(), requestQueueId);

		if (res != 1) {
			throw new Exception();
		}

	}

	public void generate() {

		int b = 10;
		int a = 0;
		while (a < b) {

			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// logger.info("counter "+ a);
			logger.info("listener invoked - Consuming Message with Identifier :" + a);
			a++;

		}
	}

	@RabbitListener(queues = "${rabbitmq.deadLetterqueue}", id = "${rabbitmqDeadLetter.listenerId}")
	public void reciveDeadLetterMessage(Message message) throws Exception {

		List<Object> map = (List<Object>) message.getMessageProperties().getHeaders().get("x-death");

		Map<String, Object> ob = (Map<String, Object>) map.get(0);
		Date requestReceiveDL = (Date) ob.get("time");

		Long requestQueueId = jdbcTemplate.queryForObject("select id from request_queue where correlation_id=?",
				Long.class, ((Message) message).getMessageProperties().getCorrelationId());

		byte[] body = ((Message) message).getBody();
		Blob blob = new javax.sql.rowset.serial.SerialBlob(body);
		Integer result = jdbcTemplate.update("INSERT INTO dead_letter_queue_request VALUES (?,?, ?, ?, ?,?,?)", null,
				message.getMessageProperties().getConsumerTag(), message.getMessageProperties().getCorrelationId(),
				null, message.getMessageProperties().getConsumerQueue(), blob, requestReceiveDL);

		if (result != 1) {
			throw new Exception();
		} else if (result == 1) {
			Long deadLetterQueueId = jdbcTemplate.queryForObject(
					"select id from dead_letter_queue_request where correlation_id=?", Long.class,
					((Message) message).getMessageProperties().getCorrelationId());
			Integer res = jdbcTemplate.update("update request_queue set dead_letter_queue_id=? where id=?",
					deadLetterQueueId, requestQueueId);
			Integer resp = jdbcTemplate.update("update dead_letter_queue_request set dlconsume_time=? where id=?",
					new Date(), deadLetterQueueId);

			if (res != 1 && resp != 1) {
				throw new Exception();
			}

		}

	}

}
