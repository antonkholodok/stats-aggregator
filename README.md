# Stats Aggregator

## Problem

* create thread-safe service that aggregates statistics in-memory for the last K (60) seconds at realtime
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

## Solution

* represent every time interval with duration of second as a bucket: `[secondSinceEpoch, secondSinceEpoch + 1)`
* bucket itself is an aggregation of all transactions that fall into bucket time range
* have array of K buckets: `buckets`
* on every new transaction request:
  * if `timestamp` is obsolete or in future - ignore it
  * calculate bucket index: `index = timestamp % K`
  * if bucket `buckets[index]` has no data - insert new bucket populated with just arrived data
  * if bucket `buckets[index]` has data and `buckets[index].secondSinceEpoch == timestamp.secondSinceEpoch` - 
  merge new transaction into existing bucket
  * if bucket `buckets[index]` has data and `buckets[index].secondSinceEpoch != timestamp.secondSinceEpoch` - 
  existing bucket is obsolete and could be ignored, insert new bucket populated with data from transaction
* on every aggregation read request:
  * iterate over `buckets`, filter buckets that fall into desired time range and accumulate them
* use CAS to avoid blocking
* see [BucketStatsAggregator](https://github.com/antonkholodok/stats-aggregator/blob/master/src/main/java/com/akholodok/stats/aggregator/service/BucketStatsAggregator.java)
* complexity:
  * for K << N - for write/read time and memory complexity is constant O(1)
  * for K -> N - for write/read time and memory complexity increases to linear O(N), so in this case another approach should be considered
* knowing read/write frequency distribution various optimizations could be applied, e.g. having majority of reads 
it would make sense to perform pre-aggregations on write operation etc.
* downside of bucketing approach - less accuracy (seconds precision) in favor of time and space complexity  