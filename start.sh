#!/usr/bin/env bash

DPE_JAR="dpe-spark-assembly-0.0.1.jar"
TASK_ID=$(awk -F. '{print $2}' <<< $MESOS_TASK_ID)
MESOS_NAME="northstar-dpe-spark-${TASK_ID}"
MESOS_ROLE="northstar-dpe-spark-${TASK_ID}"
MESOS_MASTER="mesos://zk://${MESOS_ZK_HOST_PORT}/mesos"
MASTER_MEM="${MASTER_MEM}g"
WORKER_MEM="${WORKER_MEM}g"
LOCAL_IP=`hostname -i`
SPARK_SUBMIT_FLAGS="--conf spark.executorEnv.MON_GROUP=$MON_GROUP --conf spark.executorEnv.MON_APP=$MON_APP --conf spark.executorEnv.MON_CONTACT=$MON_CONTACT \
                    --conf spark.executorEnv.MON_CORELATIONID=$MON_CORELATIONID --conf spark.executorEnv.MESOS_TASK_ID=$TASK_ID --conf spark.executorEnv.STATS_INTERVAL=$STATS_INTERVAL \
                    --conf spark.executorEnv.LOGGER_ZOOKEEPER_HOST_PORT=$LOGGER_ZOOKEEPER_HOST_PORT --conf spark.executorEnv.LOGGER_KAFKA_BROKERS_HOST_PORT=$LOGGER_KAFKA_BROKERS_HOST_PORT \
                    --conf spark.executorEnv.DKT_LOGGER_IS_KAFKA_ENABLED=$DKT_LOGGER_IS_KAFKA_ENABLED --conf spark.executorEnv.DKT_LOGGER_DUMP_MSG_STDOUT=$DKT_LOGGER_DUMP_MSG_STDOUT \
                    --conf spark.driver.host=$LOCAL_IP --conf spark.mesos.role=$MESOS_ROLE --conf spark.mesos.executor.docker.image=$DOCKER_IMAGE \
                    --conf spark.driver.cores=$MASTER_CORES --conf spark.driver.memory=$MASTER_MEM --conf spark.cores.max=$CORES_MAX \
                    --conf spark.executor.memory=$WORKER_MEM --conf spark.mesos.uris=$MESOS_URIS"
$SPARK_HOME/bin/spark-submit --name $MESOS_NAME --deploy-mode client $SPARK_SUBMIT_FLAGS --master "$MESOS_MASTER" $DPE_HOME/$DPE_JAR --class com.verizon.northstar.dpe.DPEMain
