package com.github.kurbatov.breeze.rest;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 *
 * @author Oleg Kurbatov &lt;o.v.kurbatov@gmail.com&gt;
 */
@RestController
@RequestMapping
public class ReadingsController {

    @Autowired
    private Cluster cassandraCluster;

    @Autowired
    private Executor taskExecutor;

    @GetMapping("/groups")
    public Mono<List<String>> getGroups() {
        Flux<String> result;
        Session session = cassandraCluster.newSession();
        ResultSetFuture resultSet = session.executeAsync("SELECT DISTINCT group_id, device_id FROM breeze.iot_messages;");
        result = Flux.create(emitter -> {
            resultSet.addListener(() -> {
                if (resultSet.isDone()) {
                    resultSet.getUninterruptibly().all().stream().map(row -> row.getString(0)).distinct().forEach(emitter::next);
                }
                emitter.complete();
                session.close();
            }, taskExecutor);
        });
        return result.collectList();
    }

    @GetMapping("/groups/{groupId}")
    public Mono<Readings> getDevicesByGroup(
            @PathVariable String groupId,
            @RequestParam(defaultValue = "AVG") String aggregation,
            @RequestParam(required = false) Long timestamp,
            @RequestParam(defaultValue = "5000") Long duration
    ) {
        final Long ts;
        if (timestamp == null) {
            ts = Instant.now().minusMillis(duration).toEpochMilli();
        } else {
            ts = timestamp;
        }
        Mono<Readings> result;
        Session session = cassandraCluster.newSession();
        Statement statement = QueryBuilder.select()
                .fcall(aggregation, QueryBuilder.column("value"))
                .from("breeze", "iot_messages")
                .allowFiltering()
                .where(QueryBuilder.eq("group_id", groupId))
                .and(QueryBuilder.gte("timestamp", ts))
                .and(QueryBuilder.lte("timestamp", ts + duration));
        ResultSetFuture resultSet = session.executeAsync(statement);
        result = Mono.create(emitter -> {
            resultSet.addListener(() -> {
                if (resultSet.isDone()) {
                    Readings r = new Readings();
                    r.setGroupId(groupId);
                    r.setAggregation(aggregation);
                    r.setTimestamp(ts);
                    r.setDuration(duration);
                    r.setValue(resultSet.getUninterruptibly().one().getDouble(0));
                    emitter.success(r);
                }
                session.close();
            }, taskExecutor);
        });
        return result;
    }

    @GetMapping("/devices")
    public Mono<List<String>> getDevices() {
        Flux<String> result;
        Session session = cassandraCluster.newSession();
        ResultSetFuture resultSet = session.executeAsync("SELECT DISTINCT group_id, device_id FROM breeze.iot_messages;");
        result = Flux.create(emitter -> {
            resultSet.addListener(() -> {
                if (resultSet.isDone()) {
                    resultSet.getUninterruptibly().all().stream().map(row -> row.getString(1)).forEach(emitter::next);
                }
                emitter.complete();
                session.close();
            }, taskExecutor);
        });
        return result.collectList();
    }

    @GetMapping("/devices/{deviceId}")
    public Mono<Readings> getDevice(
            @PathVariable String deviceId,
            @RequestParam(defaultValue = "AVG") String aggregation,
            @RequestParam(required = false) Long timestamp,
            @RequestParam(defaultValue = "5000") Long duration
    ) {
        final Long ts;
        if (timestamp == null) {
            ts = Instant.now().minusMillis(duration).toEpochMilli();
        } else {
            ts = timestamp;
        }
        Mono<Readings> result;
        Session session = cassandraCluster.newSession();
        Statement statement = QueryBuilder.select()
                .fcall(aggregation, QueryBuilder.column("value"))
                .column("group_id")
                .from("breeze", "iot_messages")
                .allowFiltering()
                .where(QueryBuilder.eq("device_id", deviceId))
                .and(QueryBuilder.gte("timestamp", ts))
                .and(QueryBuilder.lte("timestamp", ts + duration));
        ResultSetFuture resultSet = session.executeAsync(statement);
        result = Mono.create(emitter -> {
            resultSet.addListener(() -> {
                if (resultSet.isDone()) {
                    Row row = resultSet.getUninterruptibly().one();
                    Readings r = new Readings();
                    r.setGroupId(row.getString(1));
                    r.setDeviceId(deviceId);
                    r.setAggregation(aggregation);
                    r.setTimestamp(ts);
                    r.setDuration(duration);
                    r.setValue(row.getDouble(0));
                    emitter.success(r);
                }
                session.close();
            }, taskExecutor);
        });
        return result;
    }

}
