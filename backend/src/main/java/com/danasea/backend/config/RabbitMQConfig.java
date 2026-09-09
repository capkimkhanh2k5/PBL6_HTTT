package com.danasea.backend.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "danasea.exchange.topic";
    public static final String DLX_EXCHANGE_NAME = "danasea.exchange.dlx";

    // Auth events queue
    public static final String NOTIFICATION_EMAIL_QUEUE = "notification.email.queue";
    public static final String NOTIFICATION_EMAIL_DLQ = "notification.email.dlq";
    public static final String USER_REGISTERED_ROUTING_KEY = "auth.user.registered";
    
    // OTP events queue
    public static final String NOTIFICATION_OTP_EMAIL_QUEUE = "notification.otp-email.queue";
    public static final String NOTIFICATION_OTP_EMAIL_DLQ = "notification.otp-email.dlq";
    public static final String USER_OTP_REQUESTED_ROUTING_KEY = "auth.user.otp_requested";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public TopicExchange dlxExchange() {
        return new TopicExchange(DLX_EXCHANGE_NAME);
    }

    @Bean
    public Queue notificationEmailQueue() {
        return QueueBuilder.durable(NOTIFICATION_EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE_NAME)
                .withArgument("x-dead-letter-routing-key", NOTIFICATION_EMAIL_QUEUE + ".dlq")
                .build();
    }

    @Bean
    public Queue notificationEmailDlq() {
        return QueueBuilder.durable(NOTIFICATION_EMAIL_DLQ).build();
    }

    @Bean
    public Binding bindingNotificationEmailQueue(Queue notificationEmailQueue, TopicExchange exchange) {
        return BindingBuilder.bind(notificationEmailQueue).to(exchange).with(USER_REGISTERED_ROUTING_KEY);
    }

    @Bean
    public Binding bindingNotificationEmailDlq(Queue notificationEmailDlq, TopicExchange dlxExchange) {
        return BindingBuilder.bind(notificationEmailDlq).to(dlxExchange).with(NOTIFICATION_EMAIL_QUEUE + ".dlq");
    }

    @Bean
    public Queue notificationOtpEmailQueue() {
        return QueueBuilder.durable(NOTIFICATION_OTP_EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE_NAME)
                .withArgument("x-dead-letter-routing-key", NOTIFICATION_OTP_EMAIL_QUEUE + ".dlq")
                .build();
    }

    @Bean
    public Queue notificationOtpEmailDlq() {
        return QueueBuilder.durable(NOTIFICATION_OTP_EMAIL_DLQ).build();
    }

    @Bean
    public Binding bindingNotificationOtpEmailQueue(Queue notificationOtpEmailQueue, TopicExchange exchange) {
        return BindingBuilder.bind(notificationOtpEmailQueue).to(exchange).with(USER_OTP_REQUESTED_ROUTING_KEY);
    }

    @Bean
    public Binding bindingNotificationOtpEmailDlq(Queue notificationOtpEmailDlq, TopicExchange dlxExchange) {
        return BindingBuilder.bind(notificationOtpEmailDlq).to(dlxExchange).with(NOTIFICATION_OTP_EMAIL_QUEUE + ".dlq");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setMandatory(true);

        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                System.err.println("Message failed to publish: " + cause);
            }
        });

        template.setReturnsCallback(returned -> {
            System.err.println("Message returned: " + returned.getMessage() + " with reply code: " + returned.getReplyCode());
        });

        return template;
    }
}
