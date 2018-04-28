package com.github.kurbatov.breeze.service;

import com.github.kurbatov.breeze.message.IoTMessage;
import com.github.kurbatov.breeze.message.IoTMessageSource;
import java.util.concurrent.Executor;
import javax.annotation.PostConstruct;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author Oleg Kurbatov &lt;o.v.kurbatov@gmail.com&gt;
 */
@Service
public class FlinkJobExecutor {

    @Autowired
    private Executor taskExecutor;

    private static final Logger LOGGER = LoggerFactory.getLogger(FlinkJobExecutor.class);

    @PostConstruct
    public void init() {
        taskExecutor.execute(() -> {
            StreamExecutionEnvironment see = StreamExecutionEnvironment.getExecutionEnvironment();
            DataStreamSource<IoTMessage> source = see.addSource(new IoTMessageSource(), "IoTMessageSource", TypeInformation.of(IoTMessage.class));
            source.timeWindowAll(Time.seconds(10), Time.seconds(1))
                    .max("value")
                    .print();
            try {
                see.execute("Breeze Intake");
            } catch (Exception e) {
                LOGGER.error("Error of a flink-job execution.", e);
            }
        });
    }
    
}
