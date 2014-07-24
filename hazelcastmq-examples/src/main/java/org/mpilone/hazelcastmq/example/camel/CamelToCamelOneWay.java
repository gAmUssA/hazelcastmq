package org.mpilone.hazelcastmq.example.camel;

import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.mpilone.hazelcastmq.camel.*;
import org.mpilone.hazelcastmq.core.*;
import org.mpilone.hazelcastmq.example.ExampleApp;
import org.slf4j.*;

import com.hazelcast.config.Config;
import com.hazelcast.core.*;

/**
 * An example of using the {@link HazelcastMQCamelComponent} to consume a
 * message from one HzMq queue and produce it to another in a one way operation.
 *
 * @author mpilone
 */
public class CamelToCamelOneWay extends ExampleApp {

  /**
   * The log for this class.
   */
  private final static Logger log = LoggerFactory.getLogger(
      CamelToCamelOneWay.class);

  public static void main(String[] args) throws Exception {
    CamelToCamelOneWay app = new CamelToCamelOneWay();
    app.runExample();
  }

  @Override
  public void start() throws Exception {

    // Create a Hazelcast instance.
    Config config = new Config();
    config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
    HazelcastInstance hazelcast = Hazelcast.newHazelcastInstance(config);

    try {
      // Create the HazelcaseMQ instance.
      HazelcastMQConfig mqConfig = new HazelcastMQConfig();
      mqConfig.setHazelcastInstance(hazelcast);
      HazelcastMQInstance mqInstance = HazelcastMQ
          .newHazelcastMQInstance(mqConfig);

      // Create the camel component.
      HazelcastMQCamelConfig mqCamelConfig = new HazelcastMQCamelConfig();
      mqCamelConfig.setHazelcastMQInstance(mqInstance);

      HazelcastMQCamelComponent mqCamelComponent =
          new HazelcastMQCamelComponent();
      mqCamelComponent.setConfiguration(mqCamelConfig);

      // Create the Camel context. This could be done via a Spring XML file.
      CamelContext camelContext = new DefaultCamelContext();
      camelContext.addComponent("hazelcastmq", mqCamelComponent);

      camelContext.addRoutes(new RouteBuilder() {
        @Override
        public void configure() {
          from("hazelcastmq:queue:primo.test")
              .to("hazelcastmq:queue:secondo.test");
        }
      });

      camelContext.start();

      // Send a message to the first queue and the Camel route should move it to the second.
      try (HazelcastMQContext mqContext = mqInstance.createContext()) {
        HazelcastMQProducer mqProducer = mqContext.createProducer();
        mqProducer.send("/queue/primo.test", "Hello World!");

        try (HazelcastMQConsumer mqConsumer =
            mqContext.createConsumer("/queue/secondo.test")) {
          HazelcastMQMessage msg = mqConsumer.receive(12, TimeUnit.SECONDS);

          if (msg == null) {
            log.warn("Did not get expected message!");
          }
          else {
            log.info("Got message on second queue: " + msg.getBodyAsString());
          }
        }
      }
      camelContext.stop();
    }
    finally {
      // Shutdown Hazelcast.
      hazelcast.getLifecycleService().shutdown();
    }
  }
}
