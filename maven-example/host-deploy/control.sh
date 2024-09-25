#!/bin/bash

CUR_DIR=$(
    cd "$(dirname "$0")"
    pwd
)

echo "CUR_DIR=$CUR_DIR"

#creat directory
if [[ ! -d "logs" ]]; then
    mkdir "logs"
fi

if [[ ! -d "update" ]]; then
    mkdir "update"
fi

if [[ ! -d "backup" ]]; then
    mkdir "backup"
fi

#load config
CTRL_CFG=${CUR_DIR}/control.config
if [[ ! -f "${CTRL_CFG}" ]]; then
    echo "Control config file[${CTRL_CFG}] not exist!"
    exit 0
else
    source ${CTRL_CFG}
fi

TODAY=$(date "+%Y-%m-%d")
CUR_TIME=$(date "+%Y%m%d_%H%M%S")
MON_LOG="$CUR_DIR/logs/monitor-$TODAY.log"
OUT_LOG="$CUR_DIR/logs/out.log"
BACK_OUT_LOG="${CUR_DIR}/logs/out_${CUR_TIME}.log"
PID_FILE=__pid__

JAVA_ARGS=

##app name
if [[ -z "${APP_NAME}" ]]; then
    APP_NAME=$(ls ${CUR_DIR}/*.jar)
fi

## boot jar
JAVA_ARGS="${JAVA_ARGS} -jar $APP_NAME"

## jvm opts
JAVA_ARGS="${JAVA_OPTS} ${JAVA_ARGS}"

## main class
if [[ -n "${MAIN_CLASS}" ]]; then
    MAIN_CLASS="-Dloader.main=${MAIN_CLASS}"
fi
JAVA_ARGS="${JAVA_ARGS} ${MAIN_CLASS}"

## app config
if [[ -n "${APP_CONF}" ]]; then
    JAVA_ARGS="${JAVA_ARGS} --spring.config.location=${APP_CONF}"
fi

# default 600 second
if [[ -z "${MON_TIME}" ]]; then
    MON_TIME=600
fi

MON_FILE_PATH=${CUR_DIR}/${MON_FILE}

function log() {
    ct=$(date "+%Y-%m-%d %H:%M:%S")
    echo "$ct $*" | tee -a ${MON_LOG}
}

source ~/.bashrc
JAVA=
if [[ -n "${JAVA_HOME}" ]]; then
    JAVA=${JAVA_HOME}/bin/java
    echo "Use JAVA_HOME : ${JAVA}"
else
    JAVA=$(which java)
    if [[ -z ${JAVA} ]]; then
        echo "java not install, please check!"
        exit 3
    fi
fi

ARGS=$1
CMD=

## show command help
function showHelp() {
    echo "Usage control.sh [start|stop|restart|status]"
    echo -e "\t start  : Start service"
    echo -e "\t stop   : Stop service"
    echo -e "\t restart: Restart service"
    echo -e "\t status : Show service status"
}

## get current process id
function currentPid() {
    if [[ -f ${PID_FILE} ]]; then
        PID=$(cat ${PID_FILE})
        ps -ef | grep java | grep ${PID} | grep -v grep | awk '{print $2}'
    fi
}

## check service status
function checkActive() {
    PID=$(currentPid)
    if [[ -z "${PID}" ]]; then
        log "Service not exist."
        return 1
    fi

    if [[ -f "${MON_FILE_PATH}" ]]; then
        lmt=$(stat -c %Y ${MON_FILE_PATH})
        ct=$(date +%s)

        dt=$(expr ${ct} - ${lmt})
        if [[ ${dt} -gt ${MON_TIME} ]]; then
            log "File[${MON_FILE_PATH}] not change from ${lmt}"
            return 1
        fi
    fi

    return 0
}

## start service
function startService() {
    PID=$(currentPid)
    if [[ ${PID} ]]; then
        log "Service[${APP_NAME}] already running,pid[${PID}]."
    else
        log "Start Service:[${APP_NAME}]"
        CMD="${JAVA} ${JAVA_ARGS}"
        log "Run cmd:[${CMD}]"
        nohup ${CMD} >${OUT_LOG} 2>&1 &
        echo "$!" >${PID_FILE}
        log "Start service done."
    fi
}

#stop service
function stopService() {
    PID=$(currentPid)
    if [[ ${PID} ]]; then
        log "Stop Service[${APP_NAME}][${PID}]"
        kill -9 ${PID}
    else
        log "Stop Service[${APP_NAME}] is not running."
    fi
}

#restart service
function restartService() {
    log "Restart service[${APP_NAME}]"
    stopService
    sleep 3
    startService
}

#show service status
function showStatus() {
    echo "Service[${APP_NAME}] status:"
    echo "*************************************************************"
    ps -ef | grep java | grep ${APP_NAME} | grep -v grep
    echo "*************************************************************"
}

#
function monitorService() {
    checkActive
    activeStatus=$?

    if [[ ${activeStatus} -eq 1 ]]; then
        log "Service state is abnormal,restart service."
        restartService
    else
        log "Service state is normal."
    fi

}

function showArgs() {
    log "Config     : ${CTRL_CFG}"
    log "Dir        : ${CUR_DIR}"
    log "App        : ${APP_NAME}"
    log "Monitor    : ${MON_FILE}(${MON_TIME})"
}

## check args not empty
if [[ -z "${ARGS}" ]]; then
    showHelp
    exit 0
fi

log "==========================================================="
log "CMD: $0 $*"
log "==========================================================="
log ""

## switch control command
case "$ARGS" in
start) startService && showStatus ;;
stop) stopService && showStatus ;;
restart) restartService && showStatus ;;
status) showStatus ;;
showArgs) showArgs ;;
monitor) monitorService ;;
*) showHelp ;;
esac

exit 0
