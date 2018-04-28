package com.github.kurbatov.breeze.message;

import com.github.kurbatov.breeze.AppStarter;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Oleg Kurbatov &lt;o.v.kurbatov@gmail.com&gt;
 */
public class IoTMessageSource extends RichParallelSourceFunction<IoTMessage> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(IoTMessageSource.class);
    
    private static final long serialVersionUID = 5602468920963665151L;

    @Override
    public synchronized void run(SourceContext<IoTMessage> ctx) throws Exception {
        IoTMessageHandler handler = AppStarter.getContext().getBean(IoTMessageHandler.class);
        ctx.markAsTemporarilyIdle();
        handler.addContext(ctx);
        wait();
        handler.removeContext(ctx);
    }

    @Override
    public synchronized void cancel() {
        notify();
    }

}
