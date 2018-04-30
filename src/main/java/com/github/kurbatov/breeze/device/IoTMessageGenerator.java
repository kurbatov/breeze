package com.github.kurbatov.breeze.device;

import com.github.kurbatov.breeze.message.IoTMessage;
import com.github.kurbatov.breeze.message.IoTMessage;

/**
 * Generates {@link IoTMessage}s according to internal logic.
 *
 * @author Oleg Kurbatov &lt;o.v.kurbatov@gmail.com&gt;
 */
public interface IoTMessageGenerator {

    /**
     * Generates new {@link IoTMessage} according to the current state of the
     * object.
     *
     * @return message that communicates current state of the object
     */
    public IoTMessage getNextMessage();

}
