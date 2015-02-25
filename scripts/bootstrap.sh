# Bring up an onyx node
# Zookeeper|HornetQ

apt-get update
apt-get install -y openjdk-7-jre

cd /opt

if [ ! -d /opt/zookeeper-3.4.6 ]; then
    wget http://www.carfab.com/apachesoftware/zookeeper/current/zookeeper-3.4.6.tar.gz
    tar zxf zookeeper-3.4.6.tar.gz

    ln -s zookeeper-3.4.6 zookeeper
    ln -s /vagrant/config/zookeeper.conf zookeeper/conf/zoo.cfg

    cp /vagrant/config/zk-upstart.conf /etc/init/zookeeper.conf

    IP=`ifconfig eth1 | sed -En 's/127.0.0.1//;s/.*inet (addr:)?(([0-9]*\.){3}[0-9]*).*/\2/p' | sed 's/\.//g'`

    mkdir -p /var/zookeeper
    echo "$IP" > /var/zookeeper/myid
fi

cd /opt

if [ ! -d /opt/hornetq-2.4.0.Final ]; then
    wget http://downloads.jboss.org/hornetq/hornetq-2.4.0.Final-bin.tar.gz

    tar xzf hornetq-2.4.0.Final-bin.tar.gz
    ln -s hornetq-2.4.0.Final hornetq
    rm -f /opt/hornetq/config/stand-alone/clustered/hornetq-configuration.xml

    ln -s /vagrant/config/hornetq.xml /opt/hornetq/config/stand-alone/clustered/hornetq-configuration.xml

    cp /vagrant/config/hornetq-upstart.conf /etc/init/hornetq.conf
fi

service zookeeper restart
service hornetq restart
