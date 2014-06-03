# HazelcastMQ

HazelcastMQ provides a simple messaging layer on top of the basic Queue and Topic data 
structures provided by Hazelcast [Hazelcast](http://www.hazelcast.com/), an in-memory 
data grid. HazelcastMQ emphasizes simplicity over performance (although performance 
should be good enough for the vast majority of use cases). 

HazelcastMQ is divided into multiple components:
* _hazelcastmq-core_: The core MQ library that provides a JMS 2.0-like API for sending
and receiving messages. Core has no dependencies on JMS and can be used as a light
weight messaging framework.
* _hazelcastmq-jms_: A JMS 1.1 implementation which maps to HazelcastMQ Core. While 
not a full implementation of the specification, connections, sessions, producers, and 
consumers on queues and topics are implemented. HazelcastMQ JMS can be used with 
the [Spring Framework's](http://www.springsource.org/spring-framework) JmsTemplate or
[Apache Camel's](http://camel.apache.org/) JMS Component to provide a drop-in replacement
for existing brokers.
* _hazelcastmq-stomp_: A [STOMP](http://stomp.github.com) 
server which maps all SEND and SUBSCRIBE commands to HazelcastMQ Core
producers and consumers. This allows non-Java components (such as C, C++, Python, Ruby, etc.)
to interact with the MQ capabilities of HazelcastMQ. HazelcastMQ STOMP Server is not
required when using the Core or JMS facilities but may be used to support a wider 
range of messaging endpoints.
* _yeti_: A [STOMP](http://stomp.github.com) server and client framework built 
on [Netty](http://netty.io/) to make it simple to build STOMP interfaces for 
existing brokers. Yeti borrows the best ideas from 
[Stampy](https://github.com/mrstampy/Stampy) and 
[Stilts](http://stilts.projectodd.org/stilts-stomp/) to provide a fast, Netty based 
STOMP frame codecs and frame handlers. Yeti is the core STOMP server 
implementation used in HazelcastMQ STOMP however it has no direct dependency 
on Hazelcast/HazelcastMQ and may be split out into a separate project in the 
future.

## Rationale

Refer to my [initial blog post](http://mikepilone.blogspot.com/2013/01/hazelcast-jms-provider.html) for now.

## Core Features (in hazelcastmq-core)

### Implemented
* Send and receive from queues, topics, and temporary queues
* Transactional message sending (per thread, not context due to Hazelcast limitation)
* Message expiration (in the consumer only)
* Request/reply pattern using correlation IDs and reply-to destinations

### Not Going to Work Any Time Soon
* Transactional message reception

## JMS Features (in hazelcastmq-jms)

### Implemented
* JMS 1.1 APIs implemented
* Send and receive from queues, topics, and temporary queues
* Transactional message sending (per thread, not session)
* Text and Bytes message types
* Message expiration (in the consumer only)
* Connection start/stop suspended consumer delivery

### Not Implemented Yet
* Object or Stream message types
* Persistence selection per message
* Queue or topic browsing
* Probably 100 other things I've missed

### Not Going to Work Any Time Soon
* Transactional message reception
* Message selectors
* Durable subscriptions
* Message priority

## STOMP Server Features (in hazelcastmq-stomp)

### Implemented
* STOMP 1.2 protocol (which is mostly backward compatible to 1.1)
* Sending and subscribing
* Multiple clients on single server
* Queue and Topic send/receive
* Header encoding/decoding of special characters
* Transactions (BEGIN, SEND, COMMIT, ABORT)

### Not Implemented Yet
* Acks (ACK, NACK)
* Heart-beat
* Protocol version negotiation
* Probably 100 other things I've missed

### Not Going to Work Any Time Soon
* Transaction message reception or ACK/NACK (i.e. always auto ACK)

## Yeti STOMP Framework Features (in yeti)

### Implemented
* STOMP 1.2 protocol (which is mostly backward compatible to 1.1)
* Simple Stomplet structure for handling frame commands
* Pure Netty configuration for endless network I/O configuration options
* STOMP client implementation support async and sync message receiption
* Frame builder for fluent frame construction
* Header encoding/decoding of special characters

### Not Implemented Yet
* Heart-beat (but probably coming soon)
* Protocol version negotiation
* Probably 100 other things I've missed

### Yeti, Really?
The name Yeti came about because:
1. All the cool variations of STOMP are already taken by other [implementations](http://stomp.github.io/implementations.html).
2. It is a fun play on words with Netty Yeti STOMP.
3. My daughter was watching Backyardigans and [this song](http://www.nickjr.com/kids-videos/backyardigans-the-yeti-stomp.html) gets stuck in your head.

## Examples

A code example of sending a message using HazelcastMQ Core is shown below. Normally
the MQ instance is created at application startup using your DI framework of choice.

    HazelcastMQConfig mqConfig = new HazelcastMQConfig();
    mqConfig.setHazelcastInstance(hz);

    HazelcastMQInstance mqInstance = HazelcastMQ
          .newHazelcastMQInstance(mqConfig);
    HazelcastMQContext mqContext = mqInstance.createContext();

    HazelcastMQMessage msg = new HazelcastMQMessage();
    msg.setContentAsString("Hello World!");
    
    HazelcastMQProducer mqProducer = mqContext.createProducer();
    mqProducer.send("/queue/example.dest", msg);
    
    mqContext.close();
    mqInstance.shutdown();

Using HazelcastMQ Core is similar to using the JMS 2.0 API (but with no 
JMS dependencies):

1. Create a HazelcastMQ instance
2. Create a HazelcastMQContext
3. Create a message producer or consumer
4. Send or receive messages

Using HazelcastMQ JMS is similar to using any JMS provider:

1. Create a connection factory
2. Create a connection
3. Create a session
4. Create a message producer or consumer
5. Send or receive messages

Using HazelcastMQ STOMP is a simple layer on the Core functionality:

1. Create a connection factory
2. Create a stomp configuration
3. Create a stomp server
4. Connect with STOMP clients

### Simple Send and Receive
This example shows a simple send and receive message pattern where both the producer 
and consumer are implemented in the same code.

View the [example](https://github.com/mpilone/hazelcastmq/blob/master/hazelcastmq-examples/src/main/java/org/mpilone/hazelcastmq/example/core/SimpleProducerConsumer.java).

### Spring JmsTemplate
Of course you can skip a lot of the JMS API by using support libraries such as Spring's 
JMSTemplate.

View the [example](https://github.com/mpilone/hazelcastmq/blob/master/hazelcastmq-examples/src/main/java/org/mpilone/hazelcastmq/example/jms/SpringJmsTemplateOneWay.java).

### Node Failure
One of the major benefits of using Hazelcast as the message transport/store is that it 
offers flexible reliability and replication options. This example shows a (local) three 
node cluster and how messages can be produced and consumed even in the event of a single 
or multiple node failure in the cluster. If you've ever worked with a clustered JMS broker 
before, you'll appreciate the simplicity of this configuration.

View the [example](https://github.com/mpilone/hazelcastmq/blob/master/hazelcastmq-examples/src/main/java/org/mpilone/hazelcastmq/example/core/NodeFailure.java).

### STOMP Send and STOMP Receive
Using the Stomp Server on top of HazelcastMQ Core allows Hazelcast to be 
used as a distributed messaging system for components written in any language. The STOMP 
protocol is lightweight and simple to understand.

View the [example](https://github.com/mpilone/hazelcastmq/blob/master/hazelcastmq-examples/src/main/java/org/mpilone/hazelcastmq/example/stomp/StompToStompOneWay.java).

### STOMP Send and JMS Receive
Using the Stomp Server on top of HazelcastMQ Core allows a STOMP frame 
to be sent from a STOMP client and then consumed by a JMS consumer as a JMS Message. 
This allows non-Java components to produce and consumer messages while allowing Java 
components to use the rich JMS API and enterprise integration patterns.

View the [example](https://github.com/mpilone/hazelcastmq/blob/master/hazelcastmq-examples/src/main/java/org/mpilone/hazelcastmq/example/stomp/StompToJmsOneWay.java).

### More Examples
There are many more examples in the 
[hazelcastmq-examples](https://github.com/mpilone/hazelcastmq/tree/master/hazelcastmq-examples/src/main/java/org/mpilone/hazelcastmq/example) 
module.

## Future Work

If there is interest I plan on continuing development of the provider to support more 
of the JMS API and feature set. Let me know what you think and what features you need most.

## Getting Builds

The source, javadoc, and binaries are available in the 
[mpilone/mvn-repo](https://github.com/mpilone/mvn-repo) GitHub repository. You
can configure Maven or Ivy to directly grab the dependencies by adding the repository:

    <repositories>
         <repository>
             <id>mpilone-snapshots</id>
             <url>https://github.com/mpilone/mvn-repo/raw/master/snapshots</url>
         </repository>
         <repository>
             <id>mpilone-releases</id>
             <url>https://github.com/mpilone/mvn-repo/raw/master/releases</url>
         </repository>
     </repositories>

And then adding the dependency:

    <dependency>
        <groupId>org.mpilone</groupId>
        <artifactId>hazelcastmq-core</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
    
If there is enough interest I could look at moving it to a standard public Maven Repository.

