package com.plantorder.explosion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plantorder.common.Channels;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
public class QueueConfig {

    @Bean
    public Queue explodeDlq() {
        return QueueBuilder.durable(Channels.EXPLODE_DLQ).build();
    }

    @Bean
    @DependsOn("explodeDlq")
    public Queue explodeQueue() {
        return QueueBuilder.durable(Channels.EXPLODE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.EXPLODE_DLQ)
                .build();
    }

    @Bean
    public Queue reserveDlq() {
        return QueueBuilder.durable(Channels.RESERVE_DLQ).build();
    }

    @Bean
    @DependsOn("reserveDlq")
    public Queue reserveQueue() {
        return QueueBuilder.durable(Channels.RESERVE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.RESERVE_DLQ)
                .build();
    }

    @Bean
    public Queue materialReleaseDlq() {
        return QueueBuilder.durable(Channels.MATERIAL_RELEASE_DLQ).build();
    }

    @Bean
    @DependsOn("materialReleaseDlq")
    public Queue materialReleaseQueue() {
        return QueueBuilder.durable(Channels.MATERIAL_RELEASE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.MATERIAL_RELEASE_DLQ)
                .build();
    }

    @Bean
    public Queue consumeDlq() {
        return QueueBuilder.durable(Channels.CONSUME_DLQ).build();
    }

    @Bean
    @DependsOn("consumeDlq")
    public Queue consumeQueue() {
        return QueueBuilder.durable(Channels.CONSUME)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.CONSUME_DLQ)
                .build();
    }

    @Bean
    public Queue floorDlq() {
        return QueueBuilder.durable(Channels.FLOOR_DLQ).build();
    }

    @Bean
    @DependsOn("floorDlq")
    public Queue floorQueue() {
        return QueueBuilder.durable(Channels.FLOOR)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.FLOOR_DLQ)
                .build();
    }

    @Bean
    public Queue floorVoidDlq() {
        return QueueBuilder.durable(Channels.FLOOR_VOID_DLQ).build();
    }

    @Bean
    @DependsOn("floorVoidDlq")
    public Queue floorVoidQueue() {
        return QueueBuilder.durable(Channels.FLOOR_VOID)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.FLOOR_VOID_DLQ)
                .build();
    }

    @Bean
    public Queue stationDlq() {
        return QueueBuilder.durable(Channels.STATION_DLQ).build();
    }

    @Bean
    @DependsOn("stationDlq")
    public Queue stationQueue() {
        return QueueBuilder.durable(Channels.STATION)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.STATION_DLQ)
                .build();
    }

    @Bean
    public Queue inspectDlq() {
        return QueueBuilder.durable(Channels.INSPECT_DLQ).build();
    }

    @Bean
    @DependsOn("inspectDlq")
    public Queue inspectQueue() {
        return QueueBuilder.durable(Channels.INSPECT)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.INSPECT_DLQ)
                .build();
    }

    @Bean
    public Queue qualityVoidDlq() {
        return QueueBuilder.durable(Channels.QUALITY_VOID_DLQ).build();
    }

    @Bean
    @DependsOn("qualityVoidDlq")
    public Queue qualityVoidQueue() {
        return QueueBuilder.durable(Channels.QUALITY_VOID)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.QUALITY_VOID_DLQ)
                .build();
    }

    @Bean
    public Queue scrapDlq() {
        return QueueBuilder.durable(Channels.SCRAP_DLQ).build();
    }

    @Bean
    @DependsOn("scrapDlq")
    public Queue scrapQueue() {
        return QueueBuilder.durable(Channels.SCRAP)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.SCRAP_DLQ)
                .build();
    }

    @Bean
    public Queue receiveDlq() {
        return QueueBuilder.durable(Channels.RECEIVE_DLQ).build();
    }

    @Bean
    @DependsOn("receiveDlq")
    public Queue receiveQueue() {
        return QueueBuilder.durable(Channels.RECEIVE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.RECEIVE_DLQ)
                .build();
    }

    @Bean
    public Queue laborDlq() {
        return QueueBuilder.durable(Channels.LABOR_DLQ).build();
    }

    @Bean
    @DependsOn("laborDlq")
    public Queue laborQueue() {
        return QueueBuilder.durable(Channels.ACCRUE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.LABOR_DLQ)
                .build();
    }

    @Bean
    public Queue costDlq() {
        return QueueBuilder.durable(Channels.COST_DLQ).build();
    }

    @Bean
    @DependsOn("costDlq")
    public Queue costQueue() {
        return QueueBuilder.durable(Channels.COST)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.COST_DLQ)
                .build();
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("*");
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}
