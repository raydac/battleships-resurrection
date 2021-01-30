//============================================================
// Author: Igor A. Maznitsa 
// EMail : rrg@forth.org.ru
// Raydac Research Group (http://www.forth.org.ru/~rrg)
//============================================================

import java.io.*;
import java.net.*;
import java.util.*;

public class GameHTTPStream extends Thread {

  Socket sckt = null;
  DataInputStream dis = null;
  DataOutputStream dos = null;
  GameHTTPServer parent = null;
  int sessionID = -1;
  int playerID = 0;
  String command = null;
  BSGameSession bsg = null;
  String packet_number = null;

  static final int pckt_header = 0xFFCAFE00;

  boolean mode_waitpacket = true;

  int[] cmnd_buffer = null;
  int[] tcmnd_buffer = null;

  boolean cmndsent = true;

  public boolean getWait() {
    return mode_waitpacket;
  }

  public int ChcksumCalculate(int arg0, int arg1, int arg2, int arg3) {
    return arg0 + arg1 + arg2 + arg3;
  }

  public synchronized boolean sendPacket(int[] packet) {
    try {
      synchronized (packet) {
        dos.writeInt(pckt_header);
        dos.writeInt(packet[0]);
        dos.writeInt(packet[1]);
        dos.writeInt(packet[2]);
        dos.writeInt(packet[3]);
        dos.writeInt(ChcksumCalculate(packet[0], packet[1], packet[2], packet[3]));
        dos.flush();
      }
    } catch (Exception er) {
      System.out.println("Error of sending packet");
      try {
        if (sckt != null) {
          sckt.close();
        }
      } catch (Exception e) {
      }
      sckt = null;
      return false;
    }
    return true;
  }

  public void setWait(boolean mode) {
    this.mode_waitpacket = mode;
  }

  public void run() {
    String ds = null;

    try {
      ds = dis.readLine();
      if (ds == null) {
        return;
      }

      //System.out.println("HTTP Req: "+ds);
      // Decoding command
      StringTokenizer st = new StringTokenizer(ds, " ");
      try {
        st.nextToken();
        command = st.nextToken();
        st.nextToken();
      } catch (NoSuchElementException e) {
        System.out.println("-=Error command=-");
        return;
      }

      command = command.toLowerCase().trim();

      String key = null;
      String value = null;

      while (true) {
        ds = dis.readLine();
        if (ds == null) {
          return;
        }

        //System.out.println("HTTP Req: "+ds);
        if (ds.length() == 0) {
          break;
        }
        st = new StringTokenizer(ds, ":");
        try {
          key = st.nextToken().trim();
          value = st.nextToken().trim();
          if (key.equalsIgnoreCase("playerID")) {
            try {
              playerID = Integer.parseInt(value);
            } catch (NumberFormatException e) {
              System.out.println("Error of value in playerID field [" + value + "]");
              return;
            }
          } else if (key.equalsIgnoreCase("pn")) {
            packet_number = value;
          } else if (key.equalsIgnoreCase("sessionID")) {
            try {
              sessionID = Integer.parseInt(value);
            } catch (NumberFormatException e) {
              System.out.println("Error of value in sessionID field [" + value + "]");
              return;
            }
          }
        } catch (NoSuchElementException e) {
          System.out.println("Error string in query [" + ds + "]");
          return;
        }
      }

      dos.write("HTTP/1.0 200 OK\r\n\r\n".getBytes());
      dos.flush();

      if (command.endsWith("/test")) {
        return;
      }

      bsg = parent.getSession(sessionID);

      if (command.endsWith("/getinstream")) {
        //System.out.println("Incomming packet from "+playerID); 
        //if (packet_number!=null) System.out.println("pn: "+packet_number);  
        if ((sessionID < 0) || (playerID == 0) || (packet_number == null)) {
          return;
        }

        if (!parent.checkUserValidation(playerID)) {
          return;
        }

        if (bsg == null) {
          GameHTTPStream gstr = (GameHTTPStream) parent.players_table.get(new Integer(playerID));
          if (gstr != null) {
            gstr.sendPacket(new int[]{BSGameSession.GC_SESSIONREMOVE, 0, 0, 0});
            return;
          }
        }

        if (!bsg.checkValidationUser(playerID)) {
          return;
        }

        long pck = -1;
        try {
          pck = Long.parseLong(packet_number);
        } catch (NumberFormatException ee) {
          System.out.println("Error format of \"PN\" field [" + packet_number + "]");
        }

        // Reading of command 
        while (true) {
          int cmm = dis.readInt();
          if (cmm == pckt_header) {
            break;
          }
        }

        try {
          synchronized (tcmnd_buffer) {
            tcmnd_buffer[0] = dis.readInt();
            tcmnd_buffer[1] = dis.readInt();
            tcmnd_buffer[2] = dis.readInt();
            tcmnd_buffer[3] = dis.readInt();
            tcmnd_buffer[4] = dis.readInt();
            if (ChcksumCalculate(tcmnd_buffer[0], tcmnd_buffer[1], tcmnd_buffer[2], tcmnd_buffer[3]) != tcmnd_buffer[4]) {
              System.out.println("Error checksum!");
              return;
            }

            cmnd_buffer[0] = tcmnd_buffer[0];
            cmnd_buffer[1] = tcmnd_buffer[1];
            cmnd_buffer[2] = tcmnd_buffer[2];
            cmnd_buffer[3] = tcmnd_buffer[3];
          }

          //System.out.println("["+cmnd_buffer[0]+"]["+cmnd_buffer[1]+"]["+cmnd_buffer[2]+"]["+cmnd_buffer[3]+"]");
        } catch (IOException ee) {
          return;
        }

        if (!bsg.checkValidationPacketNumberForPlayer(playerID, pck)) {
          return;
        }

        switch (cmnd_buffer[0]) {
          case BSGameSession.GC_LOCKPACKET: {
            if (playerID == 0) {
              break;
            }
            bsg.setPlayerWaitMode(playerID, false);
          }
          break;
          case BSGameSession.GC_WAITPACKET: {
            if (playerID == 0) {
              break;
            }
            bsg.setPlayerWaitMode(playerID, true);
          }
          break;
          case BSGameSession.GC_EXIT: {
            if (!parent.dyn_playerID) {
              parent.removeSessionFromUser(playerID);
            }
            bsg.sendDataPacket(playerID, cmnd_buffer, sessionID, true);
            bsg.close();
            return;
          }
          default: {
            synchronized (cmnd_buffer) {
              if (!bsg.sendDataPacket(playerID, cmnd_buffer, sessionID, true)) {
                bsg.close();
                return;
              }
            }
          }
          break;
        }
      } else if (command.endsWith("/getoutstream")) {
        int arg0 = -1;
        int arg1 = -1;
        int arg2 = -1;
        int arg3 = -1;

        if (sessionID < 0) {
          // server in pause?
          if (parent.IsServerPause()) {
            try {
              dos.writeInt(pckt_header);
              this.dos.writeInt(BSGameSession.GC_SERVERPAUSE);
              this.dos.writeInt(0);
              this.dos.writeInt(0);
              this.dos.writeInt(0);
              this.dos.writeInt(ChcksumCalculate(BSGameSession.GC_SERVERPAUSE, 0, 0, 0));
              this.dos.flush();
              return;
            } catch (IOException ee) {
            }
            return;
          }

          //is the first query and required SessionID
          bsg = parent.getWaitOrEmptySesssion();
          if (bsg == null) {
            try {
              dos.writeInt(pckt_header);
              this.dos.writeInt(BSGameSession.GC_SERVEROVERLADEN);
              this.dos.writeInt(0);
              this.dos.writeInt(0);
              this.dos.writeInt(0);
              this.dos.writeInt(ChcksumCalculate(BSGameSession.GC_SERVEROVERLADEN, 0, 0, 0));
              this.dos.flush();
              return;
            } catch (IOException ee) {
            }
          } else {
            if (bsg.isEmpty()) {
              if (!parent.dyn_playerID) {
                parent.setSessionToUser(playerID, bsg);
              }
              bsg.activate(playerID);
              this.dos.writeInt(pckt_header);
              this.dos.writeInt(BSGameSession.GC_NEWSESSION);
              arg0 = BSGameSession.GC_NEWSESSION;
              this.dos.writeInt(bsg.sessionID);
              arg1 = bsg.sessionID;
              this.sessionID = bsg.sessionID;
              if (bsg.player1IsFirstMoving) {
                this.dos.writeInt(0xFFFFFFFF);
                arg2 = 0xFFFFFFFF;
              } else {
                this.dos.writeInt(0);
                arg2 = 0;
              }
            } else {
              if (!bsg.join(playerID, sessionID)) {
                if (!parent.dyn_playerID) {
                  parent.setSessionToUser(playerID, bsg);
                }
                bsg.activate(playerID);
                this.dos.writeInt(pckt_header);
                this.dos.writeInt(BSGameSession.GC_NEWSESSION);
                arg0 = BSGameSession.GC_NEWSESSION;
                this.dos.writeInt(bsg.sessionID);
                arg1 = bsg.sessionID;
                this.sessionID = bsg.sessionID;
                if (bsg.player1IsFirstMoving) {
                  this.dos.writeInt(-1);
                  arg2 = -1;
                } else {
                  this.dos.writeInt(0);
                  arg2 = 0;
                }
              } else {
                if (!parent.dyn_playerID) {
                  parent.setSessionToUser(playerID, bsg);
                }
                this.dos.writeInt(pckt_header);
                this.dos.writeInt(BSGameSession.GC_JOINTOSESSION);
                arg0 = BSGameSession.GC_JOINTOSESSION;
                this.dos.writeInt(bsg.sessionID);
                arg1 = bsg.sessionID;
                this.sessionID = bsg.sessionID;
                if (!bsg.player1IsFirstMoving) {
                  this.dos.writeInt(-1);
                  arg2 = -1;
                } else {
                  this.dos.writeInt(0);
                  arg2 = 0;
                }
              }
            }

            this.dos.writeInt(0);
            arg3 = 0;
            this.dos.writeInt(ChcksumCalculate(arg0, arg1, arg2, arg3));
            this.dos.flush();
          }
        }

        parent.registerPlayerOutStream(this);
        System.out.println("Registration of user stream for player " + playerID);
        while (true) {
          try {
            int ttt = dis.read();
            if (ttt < 0) {
              break;
            }
          } catch (IOException en) {
            try {
              if (dis != null) {
                dis.close();
              }
            } catch (IOException ee) {
            }
            dis = null;
            break;
          }
        }
        parent.removePlayerOutStream(this);
        bsg.setTimeLostConnection(playerID);
        System.out.println("Removed user stream for player " + playerID);
        try {
          if (dos != null) {
            dos.close();
          }
          if (sckt != null) {
            sckt.close();
          }
        } catch (IOException ee) {
        }
        dos = null;
        sckt = null;

        System.out.println("Player " + playerID + " connection is lost");
      } else {
        System.out.println("Error command [" + command + "]");
        return;
      }
    } catch (IOException ee) {
      System.out.println(">>>IOException !!!");
    } finally {
      try {
        if (dis != null) {
          dis.close();
        }
        if (dos != null) {
          dos.close();
        }
        if (sckt != null) {
          sckt.close();
        }
      } catch (IOException e) {
      }
    }
  }

  public GameHTTPStream(Socket client, GameHTTPServer parent) {
    cmnd_buffer = new int[4];
    tcmnd_buffer = new int[5];

    sckt = client;
    this.parent = parent;
    try {
      dis = new DataInputStream(sckt.getInputStream());
      dos = new DataOutputStream(sckt.getOutputStream());
    } catch (IOException ee) {
      System.out.println("!!!IOException in Thread start");
    }
    this.start();
  }
}
