#!/bin/bash

PROJECT_DIR=$(realpath $BASH_SOURCE | xargs dirname)

if [ -z "$PROJECT_DIR" ]
then
      echo "\$PROJECT_DIR is empty"
      exit 2
fi

echo detected project folder $PROJECT_DIR

if [ -z "$JDK_HOME" ]
then
    echo JDK_HOME not provided, using default one
    JDK_HOME='/home/igorm/SDK/jdk1.5'
fi


MIDLET_NAME=BattleShip

MIDLET_TMPCLASSES=$PROJECT_DIR/target/tmpclasses
MIDLET_CLASSES=$PROJECT_DIR/target/classes
MIDLET_PROJECT=$PROJECT_DIR/BattleShip
TARGET_JAR=$PROJECT_DIR/target/$MIDLET_NAME.jar
TARGET_JAD=$PROJECT_DIR/target/$MIDLET_NAME.jad
TARGET_MIDLET_ZIP=$PROJECT_DIR/target/BattleShip_A008.zip

WTK_LIBS=$PROJECT_DIR/libs


echo creating folders


rm -rf $MIDLET_TMPCLASSES
rm -rf $MIDLET_CLASSES
rm $TARGET_JAR
rm $TARGET_JAD

mkdir -p $MIDLET_TMPCLASSES
if [[ $? -ne 0 ]] ; then
    exit 1
fi

mkdir -p $MIDLET_CLASSES
if [[ $? -ne 0 ]] ; then
    exit 1
fi

echo compiling

$JDK_HOME/bin/javac -source 1.3 -target 1.1 -g:none -bootclasspath $WTK_LIBS/emptyapi.zip -d $MIDLET_TMPCLASSES -classpath $MIDLET_TMPCLASSES $MIDLET_PROJECT/src/com/gamefederation/playmaker/client/j2me/*.java
if [[ $? -ne 0 ]] ; then
    exit 1
fi


$JDK_HOME/bin/javac -source 1.3 -target 1.1 -g:none -bootclasspath $WTK_LIBS/emptyapi.zip -d $MIDLET_TMPCLASSES -classpath $MIDLET_TMPCLASSES $MIDLET_PROJECT/src/com/raydac/j2me/midlets/battleship/*.java
if [[ $? -ne 0 ]] ; then
    exit 1
fi

echo preverify

$PROJECT_DIR/tools/preverify -classpath $WTK_LIBS/emptyapi.zip:$MIDLET_TMPCLASSES -d $MIDLET_CLASSES $MIDLET_TMPCLASSES
if [[ $? -ne 0 ]] ; then
    exit 1
fi

echo packing classes
$JDK_HOME/bin/jar cmf $MIDLET_PROJECT/MANIFEST.MF $TARGET_JAR -C $MIDLET_CLASSES .
if [[ $? -ne 0 ]] ; then
    exit 1
fi

echo packing resources
$JDK_HOME/bin/jar umf $MIDLET_PROJECT/MANIFEST.MF $TARGET_JAR -C $MIDLET_PROJECT/res .
if [[ $? -ne 0 ]] ; then
    exit 1
fi

echo packing icon
$JDK_HOME/bin/jar uf $TARGET_JAR -C $MIDLET_PROJECT Icon.png
if [[ $? -ne 0 ]] ; then
    exit 1
fi

echo creating JAD

JAR_SIZE=$(wc -c $TARGET_JAR | cut -d " " -f1)

echo MIDlet-1: BattleShip, /Icon.png, com.raydac.j2me.midlets.battleship.BattleShip > $TARGET_JAD
echo MIDlet-Data-Size: 20000 >> $TARGET_JAD
echo MIDlet-Description: Battleships game client >> $TARGET_JAD
echo MIDlet-Jar-Size: $JAR_SIZE >> $TARGET_JAD
echo MIDlet-Jar-URL: battleship.jar >> $TARGET_JAD
echo MIDlet-Name: Battleship >> $TARGET_JAD
echo MIDlet-Permissions: javax.microedition.io.Connector.http,javax.microedition.io.Connector.https,javax.microedition.io.Connector.comm >> $TARGET_JAD
echo MIDlet-Vendor: Gamefederation >> $TARGET_JAD
echo MIDlet-Version: 1.0 >> $TARGET_JAD
echo Manifest-Version: 1.0 >> $TARGET_JAD
echo MicroEdition-Configuration: CLDC-1.0 >> $TARGET_JAD
echo MicroEdition-Profile: MIDP-1.0 >> $TARGET_JAD

echo zipping
zip -j -r $TARGET_MIDLET_ZIP $TARGET_JAD $TARGET_JAR
