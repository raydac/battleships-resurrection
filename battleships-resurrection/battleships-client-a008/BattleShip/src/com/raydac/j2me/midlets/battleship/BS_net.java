//============================================================
// Author: Igor A. Maznitsa
// EMail : rrg@forth.org.ru
// Raydac Research Group (http://www.forth.org.ru/~rrg)
//============================================================
package com.raydac.j2me.midlets.battleship;

import javax.microedition.lcdui.*;
import javax.microedition.io.*;
import java.io.*;
import java.util.*;

class BS_net
{
		static final int pckt_header = 0xFFCAFE00;
	
	//====Game commands====
		public static final int GC_NONE = 1; 
		public static final int GC_JOINTOSESSION = 2; 
		public static final int GC_NEWSESSION = 3;
		public static final int GC_EXIT = 4;
		public static final int GC_OPPONENTLOST = 5;
		public static final int GC_SESSIONREMOVE = 6;
		public static final int GC_GAMEMOVE = 7;
		public static final int GC_GAMERESULT = 8;
		public static final int GC_OPPONENTJOIN = 9;
		public static final int GC_PAUSE = 10;
		public static final int GC_WAITMOVING = 11;
		public static final int GC_OK = 12;
		public static final int GC_WAITPACKET = 13;
		public static final int GC_LOCKPACKET = 14;
		public static final int GC_SERVEROVERLADEN = 15;
		public static final int GC_GAME = 16;
		public static final int GC_SERVERPAUSE = 17;
		public static final int GC_SERVERSTART = 18;
		public static final int GC_NETERROR = 19;
		//=====================
    
    public static final String stringOpponentLost   = "Opponent lost in net";
    public static final String stringOpponentLeaved = "Opponent leaved the game";
    public static final String stringServerNotFound = "Game server are not found";
    public static final String stringServerOverladen= "Game server are overladden";
    public static final String stringSessionRemoved = "Session are removed";
    public static final String stringNetError = "Net error!";
    
    long packet_counter = 0;
	boolean lockpacket = false;    
	
	public int ChcksumCalculate(int arg0,int arg1,int arg2,int arg3)
	{
		return arg0+arg1+arg2+arg3;
	}

	
    // Permanet thread for recieving game data from server
    class HTTPInputThread extends Thread
    {
        HttpConnection in_connection;
        DataInputStream is;
        
        public void close()
        {
            work_flag = false;
            try
            {
                is.close();
            }
            catch(Exception ee){};
            is = null;
            
            try
            {
                in_connection.close();
            }
            catch(IOException k){}
            in_connection = null;
        }
        
        public HTTPInputThread()
        {}
        
        public void run()
        {
            while(work_flag)
            {
                boolean lflag=true;
				for(int att=0;att<10;att++)
                {
                    if (!work_flag) return;
                    try
                    {
                        //System.out.println("Connection to the game srver");
                        in_connection = (HttpConnection) Connector.open(url+"/getoutstream");
                        in_connection.setRequestMethod(HttpConnection.POST);
                        in_connection.setRequestProperty("playerID",playerID);
                        if (sessionID!=null) in_connection.setRequestProperty("sessionID",sessionID);
                        is = new DataInputStream(in_connection.openInputStream());
                        lflag = false;
                        break;
                    }
                    catch(IOException u)
                    {
                        System.out.println("Error of creating  connection, repeat after 3 sec");
                    }
                    try
                    {
                        Thread.sleep(3000);
                    }
                    catch(InterruptedException e)
                    {}
                }
                
                if (lflag)
                {
                    server_notfound = true;
                    return;
                }
                
                while(work_flag)
                {
					try
                    {
						while(work_flag)
						{
							int ch = is.readInt();
							if (ch==pckt_header) break;
						}

						tcmnd_buffer[0] = is.readInt();
                        tcmnd_buffer[1] = is.readInt();
                        tcmnd_buffer[2] = is.readInt();
                        tcmnd_buffer[3] = is.readInt();
						tcmnd_buffer[4] = is.readInt();
						//System.out.println("Incoming packet ["+tcmnd_buffer[0]+","+tcmnd_buffer[1]+","+tcmnd_buffer[2]+","+tcmnd_buffer[3]+"]");
						if (ChcksumCalculate(tcmnd_buffer[0],tcmnd_buffer[1],tcmnd_buffer[2],tcmnd_buffer[3])!=tcmnd_buffer[4])
						{
							tcmnd_buffer[0] = GC_NETERROR; 
							System.out.println("Error check code!");
						}
						
                    }
                    catch(IOException e)
                    {
                        break;
                    }
					synchronized(cmnd_buffer)
					{
						cmnd_buffer[0] = tcmnd_buffer[0];
						cmnd_buffer[1] = tcmnd_buffer[1];
						cmnd_buffer[2] = tcmnd_buffer[2];
						cmnd_buffer[3] = tcmnd_buffer[3];
					}
					
                    if (cmnd_buffer[0]==BS_net.GC_NONE) continue;

				    lockpacket = true;
					
					while(lockpacket&&work_flag)
					{	
						try
						{
							Thread.sleep(5);
						}
						catch(InterruptedException ee)
						{
							return;	
						}
					}
                }
                
                try
                {
                    if (is!=null) is.close();
                    if (in_connection!=null) in_connection.close();
                }
                catch(IOException e){}
            }
        }
    }
    
    String url=null;
    HTTPInputThread inp_t=null;
    boolean work_flag;
    String playerID=null;
    String sessionID=null;
    
    int [] tcmnd_buffer = null;
    int [] cmnd_buffer = null;
	
    boolean server_notfound = false;
    
    int lasterror = -1;
    
    public int getLastError()
    {
        return lasterror;
    }
    
    public void setSessionID(int id)
    {
        sessionID = Integer.toString(id);
    }
    
    public BS_net(String url,int pl_id) throws IOException
    {
        packet_counter = 0;
        playerID = Integer.toString(pl_id);
        work_flag = true;
        this.url = url;
        cmnd_buffer  = new int [4];
        tcmnd_buffer  = new int [5];
        sessionID=null;
        try
        {
            // ===Test of avability of the game server===
            HttpConnection in_connection = (HttpConnection) Connector.open(url+"/test");
            InputStream iis = in_connection.openInputStream();
            iis.read();
            iis.close();
            iis = null;
            in_connection.close();
            //===============================
        }
        catch(ConnectionNotFoundException rr)
        {
            server_notfound = true;
            throw new IOException();
        }
        
		clearBuffer();
        inp_t = new HTTPInputThread();
        inp_t.start();
    }
    
    public synchronized void sendDataPacket(int cmnd,int arg1,int arg2,int arg3) throws BS_netException
    {
        if (server_notfound) throw new BS_netException(stringServerNotFound);
        
        HttpConnection out_connection=null;
        DataOutputStream osss=null;

        try
        {
            out_connection = (HttpConnection) Connector.open(url+"/getinstream");
            out_connection.setRequestMethod(HttpConnection.POST);

			out_connection.setRequestProperty("playerID",playerID);
			out_connection.setRequestProperty("pn",Long.toString(packet_counter));
            if (sessionID!=null) out_connection.setRequestProperty("sessionID",sessionID);
            
            osss = out_connection.openDataOutputStream();
			//System.out.println("NET: Send packet ["+cmnd+","+arg1+","+arg2+","+arg3);
			
			osss.writeInt(pckt_header); 
            osss.writeInt(cmnd);
            osss.writeInt(arg1);
            osss.writeInt(arg2);
            osss.writeInt(arg3);

			osss.writeInt(ChcksumCalculate(cmnd,arg1,arg2,arg3));  
			
			osss.flush();
            try
            {
                out_connection.openInputStream();
            }
            catch(IOException jj)
			{}
			packet_counter++;
        }
        catch(IOException e)
        {
            System.out.println("Error in sendDataPacket!");
            throw new BS_netException(stringServerNotFound);
        }
        finally
        {
            try
            {
                if (osss!=null) osss.close();
                if (out_connection!=null) out_connection.close();
            }
            catch(IOException en){}
			out_connection = null;
			osss = null;
        }
    }
    
    public void clearBuffer()
    {
            synchronized(cmnd_buffer)
            {
                cmnd_buffer[0] = GC_NONE;
                cmnd_buffer[1] = 0;
                cmnd_buffer[2] = 0;
                cmnd_buffer[3] = 0;
	            lockpacket = false;	
			}
    }
    
    public void sessionClose()
    {
        try
        {
            sendDataPacket(GC_EXIT,0,0,0);
        }
        catch(BS_netException e)
        {}
    }
 
	public static int getCmnd(BS_net nc,int num)
	{
		if (nc!=null) return nc.cmnd_buffer[num]; 
		else
			return GC_NONE; 
	}
	
    public void close()
    {
		packet_counter = 0;
		work_flag = false;
        
        if (inp_t!=null) inp_t.close();
        inp_t = null;
        
        System.out.println("Net stream are closed");
    }
    
}