package com.mind.smg.smppas.queue;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitMqConfig {

	@Value("${rabbitmq.queue}")
	private String queueName;
	@Value("${rabbitmq.exchange}")
	private String exchange;
	@Value("${rabbitmq.deadLetterqueue}")
	private String deadLetterqueue;
	@Value("${rabbitmq.deadLetterexchange}")
	private String deadLetterexchange;
	@Value("${rabbitmq.deadLetterRoutingKey}")
	private String deadLetterRoutingKey;
	@Value("${rabbitmq.routingkey}")
	private String routingkey;

	@Bean
	DirectExchange deadLetterExchange() {

		return new DirectExchange(deadLetterexchange);

	}

	@Bean
	DirectExchange exchange() {

		return new DirectExchange(exchange);

	}

	@Bean
	Queue dlq() {

		return QueueBuilder.durable(deadLetterqueue).build();

	}

	@Bean
	Queue queue() {

		return QueueBuilder.durable(queueName).withArgument("x-dead-letter-exchange", deadLetterexchange)

				.withArgument("x-dead-letter-routing-key", deadLetterRoutingKey).build();

	}

	@Bean
	Binding DLQbinding() {

		return BindingBuilder.bind(dlq()).to(deadLetterExchange()).with(deadLetterRoutingKey);

	}

	@Bean
	Binding binding() {

		return BindingBuilder.bind(queue()).to(exchange()).with(routingkey);

	}

	@Bean
	public MessageConverter jsonMessageConverter() {

		return new Jackson2JsonMessageConverter();

	}

	public AmqpTemplate rabbitTemplate(ConnectionFactory connectionFactory) {

		final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);

		rabbitTemplate.setMessageConverter(jsonMessageConverter());

		return rabbitTemplate;

	}

}
