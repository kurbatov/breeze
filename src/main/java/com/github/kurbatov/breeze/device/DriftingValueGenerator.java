package com.github.kurbatov.breeze.device;

import com.github.kurbatov.breeze.message.IoTMessage;
import java.util.UUID;

/**
 *
 * @author Oleg Kurbatov &lt;o.v.kurbatov@gmail.com&gt;
 */
public class DriftingValueGenerator implements IoTMessageGenerator {

    private final String deviceId;
    
    private final String groupId;
    
    private double currentValue = 80;

    public DriftingValueGenerator(String groupId, String deviceId) {
        this.groupId = groupId;
        this.deviceId = deviceId;
    }
    
    public DriftingValueGenerator(String deviceId, String groupId, double initialValue) {
        this(deviceId, groupId);
        currentValue = initialValue;
    }
    
    @Override
    public IoTMessage getNextMessage() {
        IoTMessage msg = new IoTMessage();
        msg.setTimestamp(System.currentTimeMillis());
        msg.setDeviceId(deviceId);
        msg.setGroupId(groupId);
        msg.setEventId(UUID.randomUUID().getLeastSignificantBits());
        msg.setValue(currentValue += Math.round(Math.random() * 10) - 5);
        return msg;
    }
    
}
