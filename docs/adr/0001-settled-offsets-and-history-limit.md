# Empty windows advance the stored Offset only once settled; History limit counts from Authorization time

Huawei devices can sync days late, so an empty response for a recent window does not mean the
window will stay empty. The stored Offset therefore only moves past an empty window once it is
older than the late-sync window (`huawei.late.sync.window`, default 14 days). Kafka Connect
only stores a source offset together with a record, so the task emits an empty heartbeat record
to `huawei.heartbeat.topic` to persist it. When a window returns data, the Offset moves to just
after the newest record straight away, since requests run sequentially per Route.

Huawei only serves a limited amount of history, counted from when the User authorized access,
not from the time of the request. The earliest collected point is therefore
`max(startDate, (authorizedAt ?: createdAt) - huawei.max.history)`. The authorizer does not yet
send an `authorizedAt` time, so `createdAt` is used until it does; for Users who re-authorized
this anchor is too early: Huawei rejects the oldest windows with 403 "The query time is out of
range.", and the connector steps the Offset forward a day at a time until it is accepted again.

## Considered Options

- Advancing the stored Offset on every empty response: loses late-synced data.
- Persisting the Offset without a record through `SourceTask` offset APIs: not available in the
  Kafka Connect version the connector runs on (7.8).
- Never storing an Offset for empty windows (as the Oura connector does): every restart re-polls
  empty stretches back to the User's start date.
- Counting the History limit back from now: simpler, but drifts from Huawei's actual limit.
