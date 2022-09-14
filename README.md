# JSpectre
This is a POC for the Spectre V1 exploit in Java.  It's a work in progress.

## AccessTime
AccessTime is a set of tools for timing the access to elements in the *observable array* (the array used to capture prohibited data in the Spectre exploit).  AccessTime also contains a function to flush the observable from cache.  Since direct methods like `cflush` cannot be used, an indirect method involving iterating through a very large auxiliary array with leading and lagging pointers effectively flushes the observable from cache.<br><br> Overall, AccessTime works (surprisingly) well.

## JSpectre
This class has two different attempts at a Spectre exploit.  Both do not work. My best guess why is, repeated failed accesses to restricted information gets optimized by the JVM, reducing the possibility of speculative execution.  Since a relatively imprecise timer is being used (compared to `rdtscp`,  for example) accumulating many memory accesses is required to differentiate between cache hits and misses.  Unfortunately, this makes the code very hot and a likely target for JIT optimization. <br><br>
Future work should focus on preventing JIT optimization, as this was required for a [Javascript Spectre POC](https://github.com/google/security-research-pocs/tree/master/spectre.js).

