package com.github.kurbatov.breeze.device;

import com.github.kurbatov.breeze.message.IoTMessage;

/**
 *
 * @author Oleg Kurbatov &lt;o.v.kurbatov@gmail.com&gt;
 */
public class DecreasingValueGenerator implements IoTMessageGenerator {

    private final String deviceId;
    
    private final String groupId;
    
    private double currentValue;
    
    private double maxValue;

    public DecreasingValueGenerator(String groupId, String deviceId) {
        this(deviceId, groupId, 100);
    }
    
    public DecreasingValueGenerator(String deviceId, String groupId, double initialValue) {
        this.groupId = groupId;
        this.deviceId = deviceId;
        currentValue = initialValue;
        maxValue = currentValue;
    }
    
    @Override
    public IoTMessage getNextMessage() {
        currentValue -= Math.round(Math.random() * 100) / 10;
        if (currentValue <= 0) {
            currentValue = maxValue;
        }
        IoTMessage msg = new IoTMessage();
        msg.setTimestamp(System.currentTimeMillis());
        msg.setDeviceId(deviceId);
        msg.setGroupId(groupId);
        msg.setValue(currentValue);
        return msg;
    }
    
}
