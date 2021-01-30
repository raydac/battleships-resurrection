#!/bin/bash

#
#    Battleships PC client with GFX multi-player game support
#    Copyright (C) 2021 Igor Maznitsa
#
#    This program is free software: you can redistribute it and/or modify
#     it under the terms of the GNU General Public License as published by
#     the Free Software Foundation, either version 3 of the License, or
#     (at your option) any later version.
#
#     This program is distributed in the hope that it will be useful,
#     but WITHOUT ANY WARRANTY; without even the implied warranty of
#     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#     GNU General Public License for more details.
#

BATTLESHIPS_HOME="$(dirname ${BASH_SOURCE[0]})"
LOG_FILE=$BATTLESHIPS_HOME/console.log
JAVA_HOME=$BATTLESHIPS_HOME/jre

#JAVA_EXTRA_GFX_FLAGS="-Dcom.sun.management.jmxremote=true -Dsun.java2d.opengl=true"
JAVA_EXTRA_GFX_FLAGS="-Dsun.java2d.opengl=true"

JAVA_FLAGS="-server -Dsun.rmi.transport.tcp.maxConnectionThreads=0 -XX:+DisableAttachMechanism -Xverify:none -Xms512m -Xmx1024m --add-opens=java.base/java.util=ALL-UNNAMED"

JAVA_RUN=$JAVA_HOME/bin/java

echo \$JAVA_RUN=$JAVA_RUN &>$LOG_FILE

echo ------JAVA_VERSION------ &>>$LOG_FILE

$JAVA_RUN -version &>>$LOG_FILE

echo ------------------------ &>>$LOG_FILE

$JAVA_RUN $JAVA_FLAGS $JAVA_EXTRA_GFX_FLAGS -Djava.library.path="$BATTLESHIPS_HOME" -jar "$BATTLESHIPS_HOME"/battleships-resurrection.jar $@ &>>$LOG_FILE&

exit 0
