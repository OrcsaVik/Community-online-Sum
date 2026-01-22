package com.github.paicoding.forum.service.notify.service.impl;

import com.github.paicoding.forum.api.model.enums.NotifyTypeEnum;
import com.github.paicoding.forum.core.common.CommonConstants;
import com.github.paicoding.forum.core.rabbitmq.RabbitmqConnection;
import com.github.paicoding.forum.core.rabbitmq.RabbitmqConnectionPool;
import com.github.paicoding.forum.core.util.JsonUtil;
import com.github.paicoding.forum.core.util.SpringUtil;
import com.github.paicoding.forum.service.notify.service.NotifyService;
import com.github.paicoding.forum.service.notify.service.RabbitmqService;
import com.github.paicoding.forum.service.user.repository.entity.UserFootDO;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * rabbitmq 生产者
 *
 * @author yihui
 * @date 2022/8/24
 */
@Slf4j
@Service
public class RabbitmqServiceImpl implements RabbitmqService {
    private Connection connection;
    private Channel channel;

    @Autowired
    private NotifyService notifyService;

    @Override
    public boolean enabled() {
        return "true".equalsIgnoreCase(SpringUtil.getConfig("rabbitmq.switchFlag"));
    }

    @PostConstruct
    public void init() {
        log.info("Initializing RabbitMQ consumer resources.");
        try {
            RabbitmqConnection rabbitmqConnection = RabbitmqConnectionPool.getConnection();
            this.connection = rabbitmqConnection.getConnection();
            this.channel = connection.createChannel();

            // Declare queue and bind it once during initialization
            channel.queueDeclare(CommonConstants.QUERE_NAME_PRAISE, true, false, false, null);
            channel.queueBind(CommonConstants.QUERE_NAME_PRAISE, CommonConstants.EXCHANGE_NAME_DIRECT, CommonConstants.QUERE_KEY_PRAISE);

            log.info("RabbitMQ consumer resources initialized successfully.");
        } catch (IOException | TimeoutException e) {
            log.error("Error initializing RabbitMQ consumer resources: {}", e.getMessage(), e);
            // Depending on the severity, you might want to rethrow or exit the application
            throw new RuntimeException("Failed to initialize RabbitMQ consumer", e);
        }
    }

    @PreDestroy
    public void closeRabbitMqResources() {
        log.info("Closing RabbitMQ channel and connection.");
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
        } catch (IOException | TimeoutException e) {
            log.error("Error closing RabbitMQ resources: {}", e.getMessage(), e);
        }
    }

    @Override
    public void publishMsg(String exchange,
                           BuiltinExchangeType exchangeType,
                           String routingKey,
                           String message) {
        try {
            //创建连接
            RabbitmqConnection rabbitmqConnection = RabbitmqConnectionPool.getConnection();
            Connection connection = rabbitmqConnection.getConnection();
            //创建消息通道
            Channel channel = connection.createChannel();
            // 声明exchange中的消息为可持久化，不自动删除
            channel.exchangeDeclare(exchange, exchangeType, true, false, null);
            // 发布消息
            channel.basicPublish(exchange, routingKey, null, message.getBytes());
            log.info("Publish msg: {}", message);
            channel.close();
            RabbitmqConnectionPool.returnConnection(rabbitmqConnection);
        } catch (InterruptedException | IOException | TimeoutException e) {
            log.error("rabbitMq消息发送异常: exchange: {}, msg: {}", exchange, message, e);
        }

    }

    @Override
    public void processConsumerMsg() {
        log.info("Begin to processConsumerMsg.");

        try {
            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                           byte[] body) throws IOException {
                    String message = new String(body, "UTF-8");
                    log.info("Consumer msg: {}", message);

                    // 获取Rabbitmq消息，并保存到DB
                    // 说明：这里仅作为示例，如果有多种类型的消息，可以根据消息判定，简单的用 if...else 处理，复杂的用工厂 + 策略模式
                    notifyService.saveArticleNotify(JsonUtil.toObj(message, UserFootDO.class), NotifyTypeEnum.PRAISE);

                    channel.basicAck(envelope.getDeliveryTag(), false);
                }
            };
            // 取消自动ack
            channel.basicConsume(CommonConstants.QUERE_NAME_PRAISE, false, consumer);
            log.info("RabbitMQ consumer started for queue: {}", CommonConstants.QUERE_NAME_PRAISE);

            // The thread will now block or continue, and messages will be delivered to the consumer.
            // No need for a while(true) loop or Thread.sleep here.

        } catch (IOException e) {
            log.error("Error starting RabbitMQ consumer: {}", e.getMessage(), e);
            // Depending on the error, you might want to re-initialize or exit.
        }
    }
}