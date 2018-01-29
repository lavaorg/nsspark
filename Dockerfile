FROM BASE_REGISTRY_URI/northstar-spark:0.11-1.6.1-hadoop2.6

RUN mkdir /opt/dpe
COPY config/log4j.properties /opt/spark/conf/
COPY config/allocation.xml /opt/spark/conf/
COPY target/scala-2.10/dpe-spark-assembly-0.0.1.jar /opt/dpe/
COPY start.sh /opt/dpe/

ENV DPE_HOME /opt/dpe
WORKDIR /opt/dpe
