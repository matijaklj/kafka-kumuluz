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

package com.kumuluz.ee.streaming.kafka.utils.streams;

import com.kumuluz.ee.streaming.common.utils.StreamProcessorFactory;
import com.kumuluz.ee.streaming.kafka.config.KafkaStreamsConfigLoader;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.kafka.streams.processor.TopologyBuilder;

import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * @author Matija Kljun
 */
public class KafkaStreamsFactory implements StreamProcessorFactory<StreamsController> {

    private static final Logger log = Logger.getLogger(KafkaStreamsFactory.class.getName());

    @Override
    public StreamsController createStreamProcessor() {
        return new StreamsController();
    }

    public void setStreamProcessor(StreamsController sb,
                                                Object instance,
                                                String configName,
                                                Method method,
                                                String id,
                                                boolean autoStart) {


        StreamsConfig streamsConfig = new StreamsConfig(KafkaStreamsConfigLoader.getConfig(configName));

        if (method.getParameterCount() > 0) {
            log.severe("StreamProcessor annotated method { " + method.getName() + " } shouldn't have any parameters!");
            return;
        }

        Class methodReturnType = method.getReturnType();

        if (!methodReturnType.equals(TopologyBuilder.class) && !methodReturnType.equals(KStreamBuilder.class)) {
            log.severe("StreamProcessor annotated method { " + method.getName() + " } must return a TopologyBuilder or KStreamBuilder instance!");
            return;
        }

        Object builderObject = null;

        try {
            builderObject = method.invoke(instance);
        } catch (Exception e) {
            log.warning("Error at invoking StreamProcessor annotated method { " + method.getName() + " } " + e.toString() + " " + e.getCause());
        }

        if (builderObject == null)
            log.warning("Error at invoking StreamProcessor annotated method { " + method.getName() + " } ");

        if (builderObject instanceof KStreamBuilder) {
            sb.setStreams((KStreamBuilder) builderObject, streamsConfig);
        } else if (builderObject instanceof TopologyBuilder) {
            sb.setStreams((TopologyBuilder) builderObject, streamsConfig);
        }

        if(autoStart) {
            try {
                sb.start();
            } catch (Exception e) {
                log.severe(e.toString());
            }
        }
    }

}
