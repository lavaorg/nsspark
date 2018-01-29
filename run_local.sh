#!/usr/bin/env bash
source $SPARK_HOME/conf/spark-env.sh

# DPE source
DPE_HOME=$NOR_HOME/go/src/github.com/northstar/dpe-spark
DPE_JAR="target/scala-2.10/dpe-spark-assembly-0.0.1.jar"

# Spark related config
DPE_SPARK_APP_NAME="northstar-dpe"
DPE_SPARK_MASTER="local[4]"
DPE_SPARK_IMAGE="$DOCKER_REGISTRY_IP_PORT_USER/northstar-spark:1.6.1-mesos-0.28.1-hadoop2.6"
DPE_SPARK_MESOS_ROLE="public"
DPE_SPARK_DRV_CORES="4"
DPE_SPARK_DRV_MEM="2g"
DPE_SPARK_CORES_MAX="100"
DPE_SPARK_EXEC_MEM="2g"
DPE_SPARK_MESOS_URIS="file:///.dockercfg"
SPARK_LOCAL_IP="localhost"

SPARK_SUBMIT_FLAGS="--conf spark.mesos.role=public --conf spark.mesos.executor.docker.image=$DOCKER_IMAGE --conf spark.driver.cores=20 --conf spark.driver.memory=20g \
                    --conf spark.cores.max=200 --conf spark.executor.memory=20g --conf spark.mesos.uris=file:///.dockercfg"
$SPARK_HOME/bin/spark-submit --master "local[4]" --packages datastax:spark-cassandra-connector:1.6.1-s_2.10 $DPE_HOME/$DPE_JAR --deploy-mode client $SPARK_SUBMIT_FLAGS --class com.verizon.northstar.dpe.DPEMain
