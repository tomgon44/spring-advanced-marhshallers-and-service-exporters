/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.util.jms;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.TransportConnector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.obm.MarshallingMessageConverter;
import org.springframework.obm.Marshaller;

/**
 * Supports integration tests with an embedded ActiveMQ instance.
 *
 * @author Josh Long
 */
abstract public class JmsIntegrationTestUtils {

    static private Log log = LogFactory.getLog(JmsIntegrationTestUtils.class);

    /**
     * during the execution of this callback, a JMS broker will be available and the {@link JmsTemplate} required to
     * connect to that JMS broker will be provided as a parameter to this
     * class's {@link JmsBrokerExecutionCallback#doWithActiveMq(org.apache.activemq.broker.BrokerService, org.springframework.jms.core.JmsTemplate)}
     */
    public static interface JmsBrokerExecutionCallback {
        void doWithActiveMq(BrokerService brokerService, JmsTemplate jmsTemplate) throws Throwable;
    }

    public static void startAndConnectToJmsBroker(Class<?> clzzForPayload, Marshaller m, JmsBrokerExecutionCallback callback) throws Throwable {
        Assert.assertNotNull("the " + JmsBrokerExecutionCallback.class.getName() + "can not be null", callback);
        String destinationUrl = "tcp://localhost:61617";
        BrokerService broker = new BrokerService();
        TransportConnector connector = broker.addConnector(destinationUrl);
        broker.start();
        while (!broker.isStarted()) {
            Thread.sleep(500);
        }
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory(destinationUrl);
        MarshallingMessageConverter converter = new MarshallingMessageConverter(clzzForPayload, m);
        JmsTemplate jmsTemplate = new JmsTemplate(activeMQConnectionFactory);
        converter.setPayloadClass(clzzForPayload);
        jmsTemplate.setMessageConverter(converter);
        jmsTemplate.afterPropertiesSet();

        try {
            callback.doWithActiveMq(broker, jmsTemplate);
        } catch (AssertionError ae) {
            throw ae;
        } catch (Throwable th) {
            if (log.isErrorEnabled()) {
                log.error("execution of jms session failed.", th);
            }
        } finally {
            connector.stop();
            broker.stop();
        }
    }
}
