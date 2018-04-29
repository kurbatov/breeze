package com.github.kurbatov.breeze.message;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.Table;

/**
 *
 * @author Oleg Kurbatov &lt;o.v.kurbatov@gmail.com&gt;
 */
@Table(keyspace = "breeze", name = "iot_messages", caseSensitiveKeyspace = true, caseSensitiveTable = true)
public class IoTMessage {
    
    @Column
    private long timestamp;
    
    @Column(name = "event_id")
    private long eventId;
    
    @Column(name = "device_id")
    private String deviceId;
    
    @Column(name = "group_id")
    private String groupId;
    
    @Column
    private double value;

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getEventId() {
        return eventId;
    }

    public void setEventId(long eventId) {
        this.eventId = eventId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.format("%s/%s: %.1f", groupId, deviceId, value);
    }
    
}
