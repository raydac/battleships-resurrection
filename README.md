![Battleships banner](assets/github_banner.png)

[![License GNU GPL3](https://img.shields.io/badge/license-GNU%20GPL%203-yellow.svg)](http://www.gnu.org/licenses/gpl.html)
[![Java 11.0+](https://img.shields.io/badge/java-11.0%2b-green.svg)](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
[![PayPal donation](https://img.shields.io/badge/donation-PayPal-cyan.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=AHWJHJFBAWGL2)
[![YooMoney donation](https://img.shields.io/badge/donation-Yoo.money-blue.svg)](https://yoomoney.ru/to/41001158080699)

> A company called Game Federation in Sweden had created games for the Palm Pilot. They had employed Russian developers. - [Mikko Honkakorpi](https://www.linkedin.com/in/mikkoh/), ["Finnish Video Games: History and Catalog"](https://www.amazon.com/Finnish-Video-Games-History-Catalog/dp/0786499621) p.93

# Changelog

## 1.1.2 (24-dec-2022)
 - PC: added AppImage PC client version for amd64
 - PC: added Launch4j wraqpped PC client version (without embedded JDK)
 - PC: embedded JDK updated to 19.0.1+11
 - PC: updated GoSDK for launcher to 1.19.4


## 1.1.1 (20-sep-2022)
 - __PC: windows launcher moved from i386 to amd64 to prevent antivirus false positive detection__
 - PC: embedded JDK updated to 17.0.4.1+1
 - PC: updated GoSDK for launcher to 1.19.1
 - PC: added MacOS DMG bundle for PC client


[Full changelog](changelog.txt)

# Disclaimer

__All Copyrights to both graphics and sound stuff belong to their authors. The project is already 20 years old, and I
can't find any footsteps of companies involved into the project. All sources and materials have been published just to
be saved for history and as an example of game programming in Java for who may concern.__

# What is it?

It is just a commemorative project in an attempt to restore and save old archived sources found on my disks.

The Battleships game project was started in the beginning of 2001 under umbrella of Ru-Soft Ltd. (Russia) by order of
Gamefederation company (Sweden) and had to be completed before the E3 2001 exhibition start. I have written pair
articles on [habr.com](https://habr.com/ru/post/458124/) and
in [my blog](http://raydac.blogspot.com/2015/05/the-battleships-game-e3-2001.html). The original project took about 2.5
months and was successfully completed in time. For me the project is remarkable one because it was my first game
experience in J2ME world (a year later I took a part in a big mobile project for a Finnish company to produce 20 mobile
games in two months).

# Screen-cast

There is short screen-cast of a game session including GFX-compatible server, restored PC-client and emulated Motorola
A008 client.

[![Link to the youtube screen-cast](assets/game-session-screenshot-youtube.jpg)](http://www.youtube.com/watch?v=f1cfqRjnRgk "Battleships screencast")

# Pre-built components

| OS                                           |  Download link                                                                                                                                                                                | 
| -------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| ![Windows](assets/icons/win64x64.png)        | __[Game client for Windows amd64 (JRE included)](https://github.com/raydac/battleships-resurrection/releases/download/1.1.2/battleships-resurrection-1.1.2-windows-jdk-amd64.zip)__           |
| ![Windows](assets/icons/win64x64.png)        | __[Game client for Windows (No JRE)](https://github.com/raydac/battleships-resurrection/releases/download/1.1.2/battleships-resurrection-1.1.2.exe)__                                         |
| ![OSX](assets/icons/macos64x64.png)          | __[Game client for OSX amd64 (JRE included)](https://github.com/raydac/battleships-resurrection/releases/download/1.1.2/battleships-resurrection-1.1.2-macos-jdk-amd64.zip)__                 |
| ![OSX Arm64](assets/icons/macosarm64x64.png) | __[Game client for OSX aarch64 (JRE included)](https://github.com/raydac/battleships-resurrection/releases/download/1.1.2/battleships-resurrection-1.1.2-macos-jdk-aarch64.zip)__             |
| ![Linux](assets/icons/linux64x64.png)        | __[Game client TAR.GZ for Linux amd64 (JRE included)](https://github.com/raydac/battleships-resurrection/releases/download/1.1.2/battleships-resurrection-1.1.2-linux-jdk-amd64.tar.gz)__     |
| ![Linux](assets/icons/appimage64x64.png)     | __[Game client AppImage for Linux amd64 (JRE included)](https://github.com/raydac/battleships-resurrection/releases/download/1.1.2/battleships-resurrection-1.1.2-x86_64.AppImage)__          |
| ![Java](assets/icons/java64x64.png)          | __[Game client, cross-platform JAR file](https://github.com/raydac/battleships-resurrection/releases/download/1.1.2/battleships-resurrection-1.1.2.jar)__                                     | 
| ![Java](assets/icons/java64x64.png)          | _[pre-built GFX-compatible server (only JAR)](https://github.com/raydac/battleships-resurrection/releases/download/1.1.2/gfx-playroom-server-1.1.2.jar)_                                      |
| ![Java](assets/icons/java64x64.png)          | _[pre-built J2ME midlet for Motorola A008 (zipped JAD+JAR)](https://github.com/raydac/battleships-resurrection/releases/download/1.1.2/BattleShip_A008.zip)_                                  |

__[Full set of pre-built components and clients on the latesr release page](https://github.com/raydac/battleships-resurrection/releases/latest)__   

# Short historical review

I took a part in the project as a Java developer and both the network communication part, and the mobile game client
were in my responsibility. The original PC game client was developed by another two developers in C++ exclusively for
Windows (it strongly required Direct3D for work).

Initially artists developed very detailed and smooth graphics and PC client got size about 160 Mb but then guys from
Gamefederation notified us that they were going to load the PC client distributive during presentation and 160 Mb was
too big size for network in 2001. As solution, the game graphics was reworked, and it started look a bit blurry. The
resulted PC client size was decreased to about 60 Mb.

## Team

All guys directly involved in management and development in 2001 (as I recall):

### Producing

- [Mark Pinan](https://www.linkedin.com/in/markpinan/)
- Alexander Dymov

### Design

- Vladimir Chernysh
- [Dmitry Kholodov](https://www.linkedin.com/in/dmitry-kholodov-39394661/)

### Sound effects

- some great game sound designer (I don't remember his name)

### C++ development

- [Sergey Kuligin](https://www.linkedin.com/in/sergeykuligin/)
- a student (I don't remember his name)

### Java development

- [Christian Andersson](https://www.linkedin.com/in/christianand/)
- [Markus Persson](https://www.linkedin.com/in/markus-persson-7282513/)
- [Igor Maznitsa](https://www.linkedin.com/in/igormaznitsa/)

# Requirements to build

## Mobile client

The mobile client is most hard part today to be built because it requires Sun WTK 2.5.2 and JDK 1.5, they both can be
still found on Oracle site, but I am not sure that the picture will be the same during next several years. I have
restored some Motorola A008 device profile for the WTK, and it can be used to get picture of working device. To be
honesty I have not ever saw working mobile client on the real device because during development it was an absolutely new
device with GPRS support and guys in Sweden had to visit the Sweden Motorola laboratory to test the client to write
report for me, it was very long way to debug.

![A008 screenshot](battleships-resurrection/battleships-client-a008/assets/emulator_screenshot.jpg)

To build the mobile client from the project root, you need use maven profile `midlet` and tune paths in the pom to your
installed JDK 1.5 and keep in mind that the `preverify` tool is presented only for Linux.

## PC client

The PC client has been totally rewritten in pure Java 11. I didn't have sources of C++ client (only graphics and sound
resources from the technical version), so that I had to restore whole game process from the scratch (but I've made an
endeavor to make it as much as possible close to the original one). The most terrible part was to calculate offsets and
ship arrangements on the field because I had not any contact with designers of the original game and many steps I made
through guesswork.

The new client is cross-platform one (in opposite to the original one which could work only under Windows with installed
Direct3D) and there are versions for Windows, Linux and MacOS. The client jar file can be started on any platform where
provided JDK 11 because it doesn't use any third-part libraries and only Swing+Java2D+JavaSound in use for media.

| Original PC client (Windows 10)                       | Restored PC client (Ubuntu 20.04)                |
| ----------------------------------------------------- | ------------------------------------------------ |
| ![Screenshot](assets/original_win_game_screenshot.jpg)|![Screenshot](assets/restored_game_screenshot.jpg)|

There is not any magic to build the PC client. The project has been formed as a regular maven project and can be build
with `mvn`, if you want build cross-platform result images then use maven profile `publish`.

## Multi-player modes

### GFX play-room

It is the original minimalistic GFX-compatible standalone play-room server which was prepared by me for E3 2001 in april 2001. I have no changes in found sources, and it contains all my one-year experience Java coding and errors. The server also formed as maven project and even can be started directly through `exec:java`.

I could not find either documents or presentations about the GFX platform on my disks. As I recall, the real
production-ready GFX server was delivered as a module for BEA WebLogic server. The standalone solution is not fully
production-ready because it was developed for restricted use in bounds of presentations but allows organising game
sessions between clients.

### LAN serverless

It is new multi-player mode allows organizing game between two PC clients in LAN (local area network). The mode requires
access for UDP-TCP ports to read-write. After start, you see in special dialog all PC clients in the LAN-network who are
ready for game, and you can send invitation to one of them or get request from them. After agreement the game will be
started.     