package com.mind.smg.smppas.queue.logic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.mind.smg.smppas.queue.dao.QueueDetailsDto;
import com.mind.smg.smppas.queue.dao.RequestDtoDto;
import com.mind.smg.smppas.queue.dao.RequestQueue;
import com.mind.smg.smppas.queue.dao.RequestQueueRepo;
import com.mind.smg.smppas.queue.dao.UnitDto;
import com.rabbitmq.client.AMQP.Queue.DeclareOk;
import com.rabbitmq.client.Channel;

@Service
public class RabitMqService {

	@Autowired
	private AmqpTemplate rabbitTemplate;

	@Value("${rabbitmq.queue}")
	private String queueName;
	@Value("${rabbitmq.exchange}")
	private String exchange;
	@Value("${rabbitmq.routingkey}")
	private String routingkey;
	@Value("${rabbitmq.deadLetterqueue}")
	private String deadLetterqueue;

	@Value("${rabbitmq.host}")
	private String host;
	@Value("${rabbitmq.virtualhost}")
	private String virtualHost;
	@Value("${spring.rabbitmq.username}")
	private String username;
	@Value("${spring.rabbitmq.password}")
	private String password;
	@Value("${rabbitmq.listenerId}")
	private String listenerId;

	@Value("${rabbitmqDeadLetter.listenerId}")
	private String deadLetterlistenerId;

	@Value("${spring.rabbitmq.listener.simple.concurrency}")
	private Integer concurrency;

	@Value("${spring.rabbitmq.listener.simple.max-concurrency}")
	private Integer maxConcurrency;

	@Value("${spring.rabbitmq.listener.simple.prefetch}")
	private Integer prefetch;

	@Autowired
	CachingConnectionFactory cachingConnectionFactory;

	@Autowired
	private RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry;

	@Autowired
	RequestQueueRepo requestQueueRepo;

	@Autowired
	JdbcTemplate jdbcTemplate;

	private static Logger logger = LogManager.getLogger(RabitMqService.class.toString());

	public void send(UnitDto unitDto) {

		try {
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

			ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);

			objectOutputStream.writeObject(unitDto);

			byte[] bytes = byteArrayOutputStream.toByteArray();

			Blob blob = new javax.sql.rowset.serial.SerialBlob(bytes);

			Message message = new Message(new Gson().toJson(unitDto).getBytes(StandardCharsets.UTF_8));

			UUID correlationId = UUID.randomUUID();
			message.getMessageProperties().setCorrelationId(correlationId.toString());
			logger.info("Sending Message to the Queue : " + unitDto.getUnitId());
			Integer result = jdbcTemplate.update("INSERT INTO request_queue VALUES (?,?, ?, ?, ?,?,?,?,?,?,?)", null,
					null, correlationId.toString(), null, null, new Date(), "NEW", blob, null, null,
					unitDto.getUserId());

			if (result != 1) {

				throw new Exception();
			} else {
				rabbitTemplate.convertAndSend(exchange, routingkey, message);
			}

			System.out.println("Submitted  task " + unitDto.getUnitId());

		} catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}

	}

	public String pauseQueue(String action, String queueName) {

		MessageListenerContainer messageListenerContainer = null;
		if (queueName.equalsIgnoreCase(deadLetterqueue)) {
			messageListenerContainer = rabbitListenerEndpointRegistry.getListenerContainer(deadLetterlistenerId);
		} else {
			messageListenerContainer = rabbitListenerEndpointRegistry.getListenerContainer(listenerId);
		}
		SimpleMessageListenerContainer simpleMessageListenerContainer = (SimpleMessageListenerContainer) messageListenerContainer;
		String response = null;

		if ("start".equalsIgnoreCase(action)) {
			simpleMessageListenerContainer.start();
			if (simpleMessageListenerContainer.getActiveConsumerCount() > 0) {
				response = "queue started successfully";
			} else {
				response = "queue failed to start";
			}

		} else if ("stop".equalsIgnoreCase(action)) {
			simpleMessageListenerContainer.stop();
			if (simpleMessageListenerContainer.getActiveConsumerCount() == 0) {
				response = "queue stopped successfully";
			} else {
				response = "queue failed to stop";
			}

		}

		return response;

	}

	public String addConsumers(Integer consumers, String queueName) {

		MessageListenerContainer messageListenerContainer = null;
		if (queueName.equalsIgnoreCase(deadLetterqueue)) {
			messageListenerContainer = rabbitListenerEndpointRegistry.getListenerContainer(deadLetterlistenerId);
		} else {
			messageListenerContainer = rabbitListenerEndpointRegistry.getListenerContainer(listenerId);
		}
		SimpleMessageListenerContainer simpleMessageListenerContainer = (SimpleMessageListenerContainer) messageListenerContainer;
		int count = simpleMessageListenerContainer.getActiveConsumerCount();
		simpleMessageListenerContainer.setMaxConcurrentConsumers(maxConcurrency);
		simpleMessageListenerContainer
				.setConcurrentConsumers(simpleMessageListenerContainer.getActiveConsumerCount() + consumers);
		simpleMessageListenerContainer.setPrefetchCount(prefetch);

		// simpleMessageListenerContainer.start();

		if (simpleMessageListenerContainer.getActiveConsumerCount() == count + consumers) {
			return consumers + " cosumers added to the queue";
		} else {
			return "failed to add consumers";
		}
	}

	public String addPrefetchCount(Integer preFetchcount, String queueName) {

		MessageListenerContainer messageListenerContainer = null;
		if (queueName.equalsIgnoreCase(deadLetterqueue)) {
			messageListenerContainer = rabbitListenerEndpointRegistry.getListenerContainer(deadLetterlistenerId);
		} else {
			messageListenerContainer = rabbitListenerEndpointRegistry.getListenerContainer(listenerId);
		}
		SimpleMessageListenerContainer simpleMessageListenerContainer = (SimpleMessageListenerContainer) messageListenerContainer;

		simpleMessageListenerContainer.stop();
		simpleMessageListenerContainer.setPrefetchCount(prefetch + preFetchcount);
		simpleMessageListenerContainer.start();
		return preFetchcount + " added";

	}

	public QueueDetailsDto getMessageCount(String queueName) {

		DeclareOk declareOk = ((RabbitTemplate) rabbitTemplate).execute(new ChannelCallback<DeclareOk>() {
			public DeclareOk doInRabbit(Channel channel) throws Exception {
				return channel.queueDeclarePassive(queueName);
			}
		});

		QueueDetailsDto queueDetailsDto = new QueueDetailsDto();
		queueDetailsDto.setConsumerCount(declareOk.getConsumerCount());
		queueDetailsDto.setMessageCount(declareOk.getMessageCount());
		queueDetailsDto.setQueueName(queueName);
		return queueDetailsDto;
	}

	public List<QueueDetailsDto> getAllQueues() {

		List<QueueDetailsDto> queueDetailsDtos = new ArrayList<QueueDetailsDto>();
		DeclareOk declareOkSmppasqueue = ((RabbitTemplate) rabbitTemplate).execute(new ChannelCallback<DeclareOk>() {
			public DeclareOk doInRabbit(Channel channel) throws Exception {
				return channel.queueDeclarePassive(queueName);
			}
		});

		QueueDetailsDto queueDetailsDto = new QueueDetailsDto();
		queueDetailsDto.setQueueName(queueName);
		queueDetailsDto.setMessageCount(declareOkSmppasqueue.getMessageCount());
		queueDetailsDto.setConsumerCount(declareOkSmppasqueue.getConsumerCount());

		DeclareOk declareOkdeadLetter = ((RabbitTemplate) rabbitTemplate).execute(new ChannelCallback<DeclareOk>() {
			public DeclareOk doInRabbit(Channel channel) throws Exception {
				return channel.queueDeclarePassive(deadLetterqueue);
			}
		});

		QueueDetailsDto detailsDto = new QueueDetailsDto();
		detailsDto.setQueueName(deadLetterqueue);
		detailsDto.setMessageCount(declareOkdeadLetter.getMessageCount());
		detailsDto.setConsumerCount(declareOkdeadLetter.getConsumerCount());

		queueDetailsDtos.add(detailsDto);
		queueDetailsDtos.add(queueDetailsDto);

		return queueDetailsDtos;
	}

	public List<RequestDtoDto> getRequestByStatus(String status) {
		List<RequestDtoDto> requestQueues = new ArrayList<>();
		List<RequestDtoDto> requestDtoDtos = new ArrayList<>();
		if (status != null) {
			requestQueues = requestQueueRepo.findByStatus(status);

			for (RequestDtoDto requestQueue : requestQueues) {

				try {
					int blobLength = (int) requestQueue.getUnitObject().length();
					byte[] blobAsBytes = requestQueue.getUnitObject().getBytes(1, blobLength);
					ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(blobAsBytes);
					ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
					UnitDto unitDto = (UnitDto) objectInputStream.readObject();
					System.out.println(unitDto);
					requestQueue.setUnitDto(unitDto);
					requestQueue.setUnitObject(null);

				} catch (IOException | SQLException | ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				requestDtoDtos.add(requestQueue);
			}

		} else {
			requestQueues = requestQueueRepo.findAllList();
			for (RequestDtoDto requestQueue : requestQueues) {

				try {
					int blobLength = (int) requestQueue.getUnitObject().length();
					byte[] blobAsBytes = requestQueue.getUnitObject().getBytes(1, blobLength);
					ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(blobAsBytes);
					ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
					UnitDto unitDto = (UnitDto) objectInputStream.readObject();
					System.out.println(unitDto);
					requestQueue.setUnitDto(unitDto);
					requestQueue.setUnitObject(null);
				} catch (IOException | SQLException | ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				requestDtoDtos.add(requestQueue);
			}

		}

		return requestDtoDtos;

	}

	public String deleteConsumers(Integer consumers, String queueName) {

		MessageListenerContainer messageListenerContainer = null;
		if (queueName.equalsIgnoreCase(deadLetterqueue)) {
			messageListenerContainer = rabbitListenerEndpointRegistry.getListenerContainer(deadLetterlistenerId);
		} else {
			messageListenerContainer = rabbitListenerEndpointRegistry.getListenerContainer(listenerId);
		}
		SimpleMessageListenerContainer simpleMessageListenerContainer = (SimpleMessageListenerContainer) messageListenerContainer;
		int count = simpleMessageListenerContainer.getActiveConsumerCount();
		int activeConsumers = simpleMessageListenerContainer.getActiveConsumerCount() - consumers;
		if (activeConsumers >= 0) {
			simpleMessageListenerContainer.setMaxConcurrentConsumers(maxConcurrency);
			simpleMessageListenerContainer
					.setConcurrentConsumers(simpleMessageListenerContainer.getActiveConsumerCount() - consumers);
			simpleMessageListenerContainer.setPrefetchCount(prefetch);

			if (simpleMessageListenerContainer.getActiveConsumerCount() == count - consumers) {
				return consumers + " cosumers deleted from the queue";
			} else {
				return "failed to delete consumers";
			}

		} else {
			return "failed to delete consumers";
		}

	}
}
