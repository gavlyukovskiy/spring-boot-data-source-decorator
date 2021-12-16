**Samples**

Run sample application on port 8081 with next endpoints:

`/commit` - returns result of `SELECT * FROM INFORMATION_SCHEMA.COLUMNS` in json, commits connection at the end.

`/rollback` - returns result of `SELECT * FROM INFORMATION_SCHEMA.COLUMNS` in json, rollbacks connection at the end.

`/query-error` - runs `SELECT UNDEFINED()` which results to an SQL error.

**P6Spy**
```
./gradlew :samples:p6spy-sample:bootRun
```

**Datasource Proxy**
```
./gradlew :samples:datasource-proxy-sample:bootRun
```

**FlexyPool**
```
./gradlew :samples:flexy-pool-sample:bootRun
```
