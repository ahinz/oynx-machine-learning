set -x

vagrant ssh node1 -c "cd /opt/kafka && sudo ./bin/kafka-topics.sh --topic input-topic --create --zookeeper localhost:2181 --partitions 3 --replication-factor 2"
vagrant ssh node1 -c "cd /opt/kafka && sudo ./bin/kafka-topics.sh --topic loud-output --create --zookeeper localhost:2181 --partitions 3 --replication-factor 2"
vagrant ssh node1 -c "cd /opt/kafka && sudo ./bin/kafka-topics.sh --topic question-output --create --zookeeper localhost:2181 --partitions 3 --replication-factor 2"
