package com.plantorder.order;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plantorder.common.Channels;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public NewTopic itemsTopic() {
        return TopicBuilder.name(Channels.ITEMS).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic bomsTopic() {
        return TopicBuilder.name(Channels.BOMS).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic ordersTopic() {
        return TopicBuilder.name(Channels.ORDERS).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic explosionsTopic() {
        return TopicBuilder.name(Channels.EXPLOSIONS).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic materialsTopic() {
        return TopicBuilder.name(Channels.MATERIALS).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic releasesTopic() {
        return TopicBuilder.name(Channels.RELEASES).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic stationsTopic() {
        return TopicBuilder.name(Channels.STATIONS).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic qualityTopic() {
        return TopicBuilder.name(Channels.QUALITY).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic scrapsTopic() {
        return TopicBuilder.name(Channels.SCRAPS).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic receiptsTopic() {
        return TopicBuilder.name(Channels.RECEIPTS).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic laborTopic() {
        return TopicBuilder.name(Channels.LABOR).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic costsTopic() {
        return TopicBuilder.name(Channels.COSTS).partitions(1).replicas(1).build();
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory(ObjectMapper objectMapper) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        JsonSerializer<Object> values = new JsonSerializer<>(objectMapper);
        values.setAddTypeInfo(true);
        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), values);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public ConsumerFactory<String, Object> consumerFactory(ObjectMapper objectMapper) {
        JsonDeserializer<Object> values = new JsonDeserializer<>(objectMapper);
        values.addTrustedPackages("*");
        values.setUseTypeHeaders(true);
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "default");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), values);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setMissingTopicsFatal(false);
        return factory;
    }
}
