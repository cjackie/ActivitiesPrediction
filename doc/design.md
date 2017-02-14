# Design document

## Database Entity Relationship Diagram

![erd](UML/ERD.jpg)

## Application Layer Protocol Specification

This experimental protocol is designed to send CSV files between two devices.
This application layer protocol is done on `TCP 9999` port. This protocol
defines three kinds of messages, `SEND`, `OK`, `ERROR`. These messges are
defined later in this document.

### Procedure

![procedure](UML/Protocol_sequence.jpg)

### Message types

#### `SEND` Message

```
SEND <length of payload> <label> [body position] \LF
<CSV file payload>
```

#### `OK` Message

```
OK \LF
```

#### `ERROR` Message

```
ERROR <length of payload>\LF
<Error message payload>
```