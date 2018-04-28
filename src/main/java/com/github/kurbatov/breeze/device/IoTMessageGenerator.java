package com.github.kurbatov.breeze.device;

import com.github.kurbatov.breeze.message.IoTMessage;
import com.github.kurbatov.breeze.message.IoTMessage;

/**
 *
 * @author Oleg Kurbatov &lt;o.v.kurbatov@gmail.com&gt;
 */
public interface IoTMessageGenerator {
    
    public IoTMessage getNextMessage();
    
}
