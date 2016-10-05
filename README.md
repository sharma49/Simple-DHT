# Simple-DHT

A simple DHT based on Chord and implementation of ID space partitioning/re-partitioning, Ring-based routing, and Node joins. The content provider implemented all DHT functionalities and supported insert, query and delete operations. On running multiple instances of app, all 5 content provider instances formed a Chord ring and served insert/query/delete requests in a distributed fashion according to the Chord protocol. ConcourseDB has been used to store key-value pairs in one record by the Content Provider instance of each avd.
