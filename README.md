## breeze
**breeze** is a proof-of-concept of IoT stream processing solution.

## Approach

When application starts it spins up Netty server which is waiting for IoT
messages (JSON over TCP/IP). The server packs received raw data (byte-streams) to
strongly-typed messages which make up the data-stream for a [Flink](https://flink.apache.org/)
job.

The Flink job just persists the messages to [Cassandra](http://cassandra.apache.org/)
cluster but could be used in order to do transformations and analysis on the fly.

Flink and Cassandra clusters are standalone in-memory services. When the application
deploys to a Flink cluster, the builtin cluster gets replaced by the real one.
Application configuration allows specifying external Cassandra cluster.

Another Netty server runs a web-service for querying collected readings. The web-service
is build as a REST service on Spring WebFlux.

## Building

The project builds with the following command line:

```
$> cd /path/to/breeze
$> mvn clean package
```

## Usage

After building the project, navigate to the distribution directory:

```
$> cd /path/to/breeze/target/dist
```

Launch stream processing using the following command:

```
$> java -jar breeze-0.0.1-SNAPSHOT.jar
```

Wait until initialization is done and launch arbitrary number of IoT device
simulations using the next command form other terminals:

```
$> java -jar breeze-0.0.1-SNAPSHOT.jar <mode> 127.0.0.1 9909
```

**breeze** currently supports several simulation modes:

- `heartrate` - simulates heart rate sensor
- `thermostat` - simulates thermostat
- `fuel` - fuel gauge

Those simulations send a message with their current state to the specified
host every second. The content of the message is rendered to the standard
output.

Web-service offers following endpoints to request collected readings:

- [http://localhost:8080/groups](http://localhost:8080/groups) and
[http://localhost:8080/devices](http://localhost:8080/devices) to request list of
known groups of devices and devices
- [http://localhost:8080/groups/{groupId}](http://localhost:8080/groups/{groupId}) and
[http://localhost:8080/devices/{deviceId}](http://localhost:8080/devices/{deviceId})
to request aggregated readings from particular group or device.

The later resources have additional request parameters:

- `aggregation` - method of value aggregation. Supported AVG (default), MIN and MAX.
- `duration` - length of a time frame for aggregation in ms. Default: 5000 ms.
- `timestamp` - start of the time frame. Default: currentTimestamp - duration.

Examples:

- [http://localhost:8080/groups/heartrate](http://localhost:8080/groups/heartrate) - average readings from all devices
from group `heartrate` for the last 5 seconds.
- [http://localhost:8080/devices/Person-21?aggregation=MAX&duration=10000](http://localhost:8080/devices/Person-21?aggregation=MAX&duration=10000) - 
maximal readings from the device with id `Person-21` for the last 10 seconds.
- [http://localhost:8080/devices/Fuel-14?timestamp=1525048365717](http://localhost:8080/devices/FuelGauge-14?timestamp=1525048365717) - 
average readings from the device with id `FuelGauge-14` for 5 seconds starting from
`1525048365717` timestamp.

## Limitations

- Monolithic standalone solution. It would be better to split to several services.
- Text-based protocol. It would be better to support a standard protocol (such as MQTT).
- No service discovery in standalone mode. (Eureka or Consul).
- Web-service does not comply with HATEOAS.
