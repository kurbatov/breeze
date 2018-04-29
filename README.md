## breeze
**breeze** is a proof-of-concept of IoT stream processing solution.

It leverages [Flink](https://flink.apache.org/) to process infinite datasets,
in-memory [Cassandra](http://cassandra.apache.org/) instance to persist
the messages.

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

Those simulations send a message with their current state every second to
the specified host. The content of the message is rendered to the standard
output.

## Limitations
