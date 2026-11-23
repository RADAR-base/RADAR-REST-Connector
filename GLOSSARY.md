# RADAR REST Connector

Kafka Connect source connectors that pull wearable data from vendor REST APIs (Fitbit, Oura,
Huawei Health Kit) into RADAR-base Kafka topics, one participant at a time.

## Language

### Collection

**Route**:
One vendor data type, collected through one request shape, into one RADAR topic.
_Avoid_: Endpoint (the vendor URL), data type (the vendor's name for it), stream

**User**:
A RADAR participant whose wearable data is collected, identified by project, user and source.
The vendor account only holds that participant's authorization.
_Avoid_: Account, subject, openId

**Offset**:
The point in time before which a User's data for a Route has been collected.
_Avoid_: Cursor, checkpoint, watermark

**Late sync**:
Data the vendor receives some time after the period it describes, because the device synced
late.

**Settled window**:
A time window old enough that no late sync is expected for it any more.
_Avoid_: Closed window, final window

**Authorization time**:
When the User last granted the connector access to their vendor account.
_Avoid_: Registration time, creation time

**History limit**:
How far before the Authorization time the vendor lets us collect a User's data.
_Avoid_: Backfill window, lookback

### Data shapes

**Sample point**:
One timestamped measurement collected for a Route.
_Avoid_: Sample, data point

**Health record**:
One vendor-grouped episode with its own detail samples, such as an ECG recording or a sleep
session.
_Avoid_: Record (too generic), event

**Daily statistics**:
One User's aggregate for a Route over a single UTC day.
_Avoid_: Summary, polymerize
