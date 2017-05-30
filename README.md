# raptor-server


This project represents the server component of the Raptor project. The server component is responsible for:

1. Consuming Raptor-AMQP Event Message (RAEM) from a configured AMQP server.
2. Enriching each RAEM from a configure attribute source.
3. Persisting the RAEM to a configured datastore (or an embedded one if no other is configured)


# Defaults

* The default memory required for raptor-server is 512m.

