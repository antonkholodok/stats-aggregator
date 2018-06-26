# Stats Aggregator

## Problem

* create thread-safe service that aggregates statistics in-memory for the last 60 seconds at realtime
* input 
```
    POST /transactions {"amount": 10.0, "timestamp": 1530048353792}
    
    amount - transaction amount,
    timestamp - transaction time in unix epoch milliseconds in UTC
``` 
* there are no sequential timestamps guarantees 
* output
```
  GET /transactions
  {
    "count": 1,
    "min": 10.0,
    "max": 10.0,
    "sum": 10.0,
    "avg": 10.0,
  } 
  
  count/min/max/sum/avg - aggregations for the last 60 seconds
```
* return status 200 for POST if transaction added
* return status 204 for POST if transaction timestamp is obsolete or in future
* return 204 for GET if there is no data for the last 60 seconds
* expected complexity: O(1) for time and memory

