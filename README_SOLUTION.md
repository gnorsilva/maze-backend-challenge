## Solution approach

The solution is written in Scala, built with Sbt and using Akka's Actors to handle the TCP connections & Akka's streams to handle the big stream of data. These are very powerful libraries yet very lightweight and concise, they seem to be perfect for this use case.

I have separated the app into 3 main actors, dividing the concerns:
- EventConnectionManager: handles the event connection, parses the event stream
- EventProcessor: handles the parsed stream, queues it, sorts it afterwards and converts it into client events
- ClientConnectionManager: handles client connections, and forwards events to clients


## Assumptions
- Having a time window for storing events in memory of 10 sec or 1M events before processing events
- No need to have configurations fed into the app, i.e. The app only listens to port 9090 & 9099, and the time windows configs are fixed
- Have to keep track in memory of who is following whom and if there is an unfollow
- Error handling is limited to the parsing of incoming data, could be more robust if needed
- Logging has been kept down to a minimum

## How to run

```bash
sbt run
```

## How to run the tests

```bash
sbt test
```