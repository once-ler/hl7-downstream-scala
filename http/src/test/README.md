## Stress tests:

```
wrk -t2 -c1000 -d 30s --timeout 10s --latency http://localhost:8080/hello
wrk -t2 -c1000 -d 30s --timeout 10s --latency http://localhost:8080/hello
```

#### With POST:

__https://github.com/wg/wrk/issues/22__

create post.lua

```
-- example HTTP POST script which demonstrates setting the
-- HTTP method, body, and adding a header

wrk.method = "POST"
wrk.body   = "foo=bar&baz=quux"
wrk.headers["Content-Type"] = "application/x-www-form-urlencoded"
```

```
wrk -t12 -c400 -d30s -s ./scripts/post.lua http://localhost:7895/hl7
```
