/*
 *  Copyright (c) 2014-2017 Kumuluz and/or its affiliates
 *  and other contributors as indicated by the @author tags and
 *  the contributor list.
 *
 *  Licensed under the MIT License (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://opensource.org/licenses/MIT
 *
 *  The software is provided "AS IS", WITHOUT WARRANTY OF ANY KIND, express or
 *  implied, including but not limited to the warranties of merchantability,
 *  fitness for a particular purpose and noninfringement. in no event shall the
 *  authors or copyright holders be liable for any claim, damages or other
 *  liability, whether in an action of contract, tort or otherwise, arising from,
 *  out of or in connection with the software or the use or other dealings in the
 *  software. See the License for the specific language governing permissions and
 *  limitations under the License.
*/

package com.kumuluz.ee.streaming.kafka.utils.consumer;

import com.kumuluz.ee.streaming.common.annotations.StreamListener;
import com.kumuluz.ee.streaming.common.utils.ConsumerFactory;
import com.kumuluz.ee.streaming.common.utils.ConsumerInitializerExtension;
import com.kumuluz.ee.streaming.common.utils.AnnotatedInstance;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeanManager;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author Matija Kljun
 */
public class KafkaConsumerInitializerExtension implements ConsumerInitializerExtension {

    private static final Logger log = Logger.getLogger(KafkaConsumerInitializerExtension.class.getName());

    ConsumerFactory<ConsumerRunnable> kafkaConsumerFactory;

    public <X> void after(@Observes AfterDeploymentValidation adv, BeanManager bm) {

        kafkaConsumerFactory = new KafkaConsumerFactory();

        // TODO remvoe this ?
        for (AnnotatedInstance inst : instanceList)
            System.out.println(inst.getMethod().getName());

        if (instanceList.size() > 0) {
            ExecutorService executor = Executors.newFixedThreadPool(instanceList.size());

            for (AnnotatedInstance<StreamListener> inst : instanceList) {

                StreamListener annotation = inst.getAnnotation();
                Method method = inst.getMethod();

                String groupId = annotation.groupId();
                String[] topics = annotation.topics();
                String configName = annotation.config();
                boolean batchListener = annotation.batchListener();

                Class<?> consumerRebalanceListener = null;
                if(annotation.consumerRebalanceListener().length != 0)
                    consumerRebalanceListener = annotation.consumerRebalanceListener()[0];

                Object instance = bm.getReference(inst.getBean(), method.getDeclaringClass(), bm
                        .createCreationalContext(inst.getBean()));

                ConsumerRunnable consumer = kafkaConsumerFactory.createConsumer(instance, configName, groupId, topics, method,
                        batchListener, consumerRebalanceListener);

                if (consumer != null) {
                    executor.submit(consumer);

                    Runtime.getRuntime().addShutdownHook(new Thread() {
                        @Override
                        public void run() {
                            consumer.shutdown();
                            executor.shutdown();
                            try {
                                executor.awaitTermination(5000, TimeUnit.MILLISECONDS);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        }
    }
}
