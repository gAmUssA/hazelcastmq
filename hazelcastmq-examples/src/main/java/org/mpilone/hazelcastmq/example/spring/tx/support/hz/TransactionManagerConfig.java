
package org.mpilone.hazelcastmq.example.spring.tx.support.hz;

import org.mpilone.hazelcastmq.spring.tx.HazelcastTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

/**
 * Spring configuration for the {@link HazelcastTransactionManager} example.
 *
 * @author mpilone
 */
@Configuration
@EnableTransactionManagement
public class TransactionManagerConfig {

  @Bean(destroyMethod = "shutdown")
  public HazelcastInstance hazelcast() {
    Config config = new Config();
    config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);

    return Hazelcast.newHazelcastInstance(config);
  }

  @Bean
  public HazelcastTransactionManager transactionManager() {
    return new HazelcastTransactionManager(hazelcast());
  }

  @Bean
  public BusinessService businessService() {
    return new BusinessServiceUsingUtils(hazelcast());
  }

  @Bean(destroyMethod = "shutdown")
  public DemoQueueReader queueReader() {
    return new DemoQueueReader(hazelcast());
  }

}
