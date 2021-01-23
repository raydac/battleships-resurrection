#!/bin/bash

BATTLESHIPS_HOME="$(dirname ${BASH_SOURCE[0]})"
LOG_FILE=$BATTLESHIPS_HOME/console.log
JAVA_HOME=$BATTLESHIPS_HOME/jre

#JAVA_EXTRA_GFX_FLAGS="-Dcom.sun.management.jmxremote=true -Dsun.java2d.opengl=true"
JAVA_EXTRA_GFX_FLAGS="-Dsun.java2d.opengl=true"

JAVA_FLAGS="-server -Dsun.rmi.transport.tcp.maxConnectionThreads=0 -XX:+DisableAttachMechanism -Xverify:none -Xms512m -Xmx1024m --add-opens=java.base/java.util=ALL-UNNAMED"

JAVA_RUN=$JAVA_HOME/bin/java

if [ -f $ZXPOLY_HOME/.pid ];
then
    SAVED_PID=$(cat $ZXPOLY_HOME/.pid)
    if [ -f /proc/$SAVED_PID/exe ];
    then
        echo Game already started! if it is wrong, just delete the .pid file in the folder root!
        exit 1
    fi
fi

echo \$JAVA_RUN=$JAVA_RUN &>$LOG_FILE

echo ------JAVA_VERSION------ &>>$LOG_FILE

$JAVA_RUN -version &>>$LOG_FILE

echo ------------------------ &>>$LOG_FILE

$JAVA_RUN $JAVA_FLAGS $JAVA_EXTRA_GFX_FLAGS -Djava.library.path="$BATTLESHIPS_HOME" -jar "$BATTLESHIPS_HOME"/battleships-resurrection.jar $@ &>>$LOG_FILE&
THE_PID=$!
echo $THE_PID>$BATTLESHIPS_HOME/.pid
wait $THE_PID
rm $BATTLESHIPS_HOME/.pid
exit 0
