# pubsub-poc

Ok. It's not actually just pubsub.

This repo is just to test out some various non-polling alternatives for a 
producer / worker architecture. The various (eventual) strategies that I'm
comparing against each other are outlined below.

Initial testing will just involve flat-out work creation and notifications,
but I intend to use this repo to also test failure scenarios, and vet out
various recovery systems.

* [x] HTTP-only 
* [ ] HTTP send, SQS notify
* [ ] HTTP send, Redis queue notify
* [ ] SQS-only
* [ ] Redis-only
* [ ] gRPC
* [ ] Kinesis send, SQS notify

## example report

```
=SQ 0007 =======================================================================
Total      93
Pending    62
Complete   31
Lost       0
Duplicates 0
-- Notify Lag (ms) -------------------------------------------------------------
Percentile     Value     Count
50.0%          7         20
75.0%          7         20
90.0%          7         20
95.0%          7         20
99.0%          1023      1
Mean = 29.61        StdDev      = 134.82
Max  = 1023         Total count = 31
=SQ 0007 =======================================================================
```
