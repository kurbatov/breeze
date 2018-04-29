package com.github.kurbatov.breeze;

import com.github.kurbatov.breeze.conf.ApplicationContextConfiguration;
import com.github.kurbatov.breeze.device.DeviceEmulator;
import com.github.kurbatov.breeze.device.DriftingValueGenerator;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

/**
 * This class starts the application.
 *
 * @author Oleg Kurbatov &lt;o.v.kurbatov@gmail.com&gt;
 */
public class AppStarter {

    private static GenericApplicationContext context;
    private static final CountDownLatch STARTING_LATCH = new CountDownLatch(1);
    private static final CountDownLatch STOPPING_LATCH = new CountDownLatch(1);
    private static final CountDownLatch FINALISATION_LATCH = new CountDownLatch(1);

    public static void main(String[] args) throws Exception {
        System.setProperty("log4j.configurationFile", "log4j2.properties");
        Thread.setDefaultUncaughtExceptionHandler(new Slf4jUncaughtExceptionHandler());
        SLF4JBridgeHandler.install();
        String mode = args.length > 0 ? args[0] : "";
        String host = args.length > 1 ? args[1] : "";
        int port = args.length > 2 ? Integer.parseInt(args[2]) : 0;
        long randomId = Math.round(Math.random() * 100);
        switch (mode) {
            case "heartrate": new DeviceEmulator(new DriftingValueGenerator(mode, String.format("Person-%d", randomId))).start(host, port); break;
            case "thermostat": new DeviceEmulator(new DriftingValueGenerator(mode, String.format("Thermostat-%d", randomId, randomId))).start(host, port); break;
            default: start(args);
        }
    }
    
    public static void start(String[] args) {
        long start = System.currentTimeMillis();
        AppStopper stopper = new AppStopper("breeze");
        if (Arrays.asList(args).stream().anyMatch(arg -> "stop".equalsIgnoreCase(arg))) {
            stopper.sendStopSignal();
            return;
        }
        Logger logger = LoggerFactory.getLogger(AppStarter.class);
        context = new AnnotationConfigApplicationContext(ApplicationContextConfiguration.class);
        context.registerBean(ParameterTool.class, () -> ParameterTool.fromArgs(args));
        context.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stop();
            waitUntilFinished();
        }));
        stopper.waitForSignal();
        long started = System.currentTimeMillis();
        logger.info("Started {} ms", started - start);
        STARTING_LATCH.countDown();
        waitUntilStopped();
        long stop = System.currentTimeMillis();
        stopper.stopWaitingForSignal();
        context.stop();
        context.close();
        logger.info("Stopped {} ms", System.currentTimeMillis() - stop);
        logger.info("Uptime {}", DurationFormatUtils.formatDurationWords(stop - started, true, true));
        LogManager.shutdown();
        finish();
    }

    /**
     * Stops the application running its finalization logic.
     */
    public static void stop() {
        STOPPING_LATCH.countDown();
    }
    
    public static GenericApplicationContext getContext() {
        try {
            STARTING_LATCH.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return context;
    }

    private static void waitUntilStopped() {
        try {
            STOPPING_LATCH.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void waitUntilFinished() {
        try {
            FINALISATION_LATCH.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void finish() {
        FINALISATION_LATCH.countDown();
    }

}
