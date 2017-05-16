# Onos Linkload monitor

A small app that monitor port statistics of switches, and computes link loads from them.
The results are published to a rabbitmq broker so that any other app can consume
the stats and use them for whatever necessary: data collection, visualization, etc.