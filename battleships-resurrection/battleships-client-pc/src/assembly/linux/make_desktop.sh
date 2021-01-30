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

# Script just generates free desktop descriptor to start application

APP_HOME="$(realpath $(dirname ${BASH_SOURCE[0]}))"
TARGET=$APP_HOME/battleships-resurrection.desktop

echo [Desktop Entry] > $TARGET
echo Version=1.1 >> $TARGET
echo Encoding=UTF-8 >> $TARGET
echo Type=Application >> $TARGET
echo Name=Battleships >> $TARGET
echo GenericName=BattleShip >> $TARGET
echo Icon=$APP_HOME/logo.svg >> $TARGET
echo Exec=\"$APP_HOME/run.sh\" %f >> $TARGET
echo Comment=RuSoft BattleShip game >> $TARGET
echo "Categories=Game;" >> $TARGET
echo "Keywords=battleships;game;" >> $TARGET
echo Terminal=false >> $TARGET
echo StartupWMClass=battleships-resurrection-pc >> $TARGET

echo Desktop script has been generated: $TARGET

if [ -d ~/.gnome/apps ]; then
    echo copy to ~/.gnome/apps
    cp -f $TARGET ~/.gnome/apps
fi

if [ -d ~/.local/share/applications ]; then
    echo copy to ~/.local/share/applications
    cp -f $TARGET ~/.local/share/applications
fi

if [ -d ~/Desktop ]; then
    echo copy to ~/Desktop
    cp -f $TARGET ~/Desktop
fi

