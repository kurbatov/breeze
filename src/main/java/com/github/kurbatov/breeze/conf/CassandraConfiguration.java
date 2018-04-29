package com.github.kurbatov.breeze.conf;

import com.datastax.driver.core.Cluster;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author Oleg Kurbatov &lt;o.v.kurbatov@gmail.com&gt;
 */
@Configuration
public class CassandraConfiguration {
    
    @Value("${cassandra.host:127.0.0.1}")
    private String cassandraHost;
    
    @Value("${cassandra.port:9142}")
    private int cassandraPort;
    
    @Bean
    public Cluster getCassandraCluster() throws Exception {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra();
        Thread.sleep(500);//cassandra doesn't manage to create file system just in time
        return Cluster.builder()
                .addContactPoints(cassandraHost)
                .withPort(cassandraPort)
                .build();
    }
    
}
