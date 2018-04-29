package com.github.kurbatov.breeze.service;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.github.kurbatov.breeze.message.IoTMessage;
import com.github.kurbatov.breeze.message.IoTMessageSource;
import java.util.concurrent.Executor;
import javax.annotation.PostConstruct;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.cassandra.CassandraSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 *
 * @author Oleg Kurbatov &lt;o.v.kurbatov@gmail.com&gt;
 */
@Service
public class FlinkJobExecutor {

    @Autowired
    private Executor taskExecutor;
    
    @Autowired
    private Cluster cassandraCluster;
    
    @Value("${cassandra.host:127.0.0.1}")
    private String cassandraHost;
    
    @Value("${cassandra.port:9142}")
    private int cassandraPort;

    private static final Logger LOGGER = LoggerFactory.getLogger(FlinkJobExecutor.class);

    @PostConstruct
    public void init() {
        taskExecutor.execute(() -> {
            try (Session session = cassandraCluster.newSession()) {
                session.execute("CREATE KEYSPACE IF NOT EXISTS breeze WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '1'};");
                session.execute("CREATE TABLE IF NOT EXISTS breeze.iot_messages ("
                        + "group_id text,"
                        + "device_id text,"
                        + "event_id bigint,"
                        + "timestamp bigint,"
                        + "value double,"
                        + "PRIMARY KEY(group_id, device_id, event_id)"
                        + ");");
            }
            StreamExecutionEnvironment see = StreamExecutionEnvironment.getExecutionEnvironment();
            DataStreamSource<IoTMessage> source = see.addSource(new IoTMessageSource(), "IoTMessageSource", TypeInformation.of(IoTMessage.class));
            try {
                CassandraSink.addSink(source.forward())
                        .setHost(cassandraHost, cassandraPort)
                        .build();
            } catch (Exception e) {
                LOGGER.error("Cannot create Cassandra sink.", e);
            }
            source.keyBy("groupId", "deviceId")
                    .timeWindow(Time.seconds(5), Time.seconds(1))
                    .maxBy("value")
                    .print();
            try {
                see.execute("Breeze Intake");
            } catch (Exception e) {
                LOGGER.error("Error of a flink-job execution.", e);
            }
        });
    }
    
    @Scheduled(initialDelay = 5000, fixedDelay = 5000)
    public void monitorStoredMessages() {
        try (Session session = cassandraCluster.newSession()) {
            LOGGER.info("{} messages stored", session.execute("SELECT COUNT(*) FROM breeze.iot_messages;").one().get(0, Long.class));
        }
    }
    
}
