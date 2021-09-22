# Battleships PC client

## Game modes

### Single player game

In the mode locally staring AI bot plays role of opponent and no any network requirements.

### Game through network

#### Old GFX server

The mode uses old GFX game server started somewhere in the network. You have to provide both the address and the port of
needed GFX server before game session start and wait connection and opponent ready.

#### Serverless network game

Since 1.1.0 version, the PC client supports serverless network game mode. It will be activated if
checkbox `Old GFX client` is off. You should select network interface to be used for connection and the port. Keep in
mind that the port should support both UDP and TCP modes and be opened by your firewall for incoming connections and
packets. After start, you will see dialog with players detected in the network, and you can send an invitation to one of
them. 
