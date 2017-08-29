package com.kumuluz.ee.streaming.kafka.utils.consumer;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;

/**
 * @author Matija Kljun
 */
public abstract class KafkaConsumerRebalanceListener implements ConsumerRebalanceListener {

    Consumer<?,?> consumer;

    public KafkaConsumerRebalanceListener(Consumer<?,?> consumer) {
        this.consumer = consumer;
    }
}
