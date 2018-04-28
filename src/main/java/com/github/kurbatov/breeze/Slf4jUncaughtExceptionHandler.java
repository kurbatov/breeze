package com.github.kurbatov.breeze;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs uncaught exceptions with a slf4j logger.
 *
 * @author Oleg Kurbatov &lt;o.v.kurbatov@gmail.com&gt;
 */
public class Slf4jUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Slf4jUncaughtExceptionHandler.class);
    
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        LOGGER.error("Uncaught exception in [{}]:\n", t.getName(), e);
    }
    
}
