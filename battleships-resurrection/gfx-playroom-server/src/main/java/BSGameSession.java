//============================================================
// Author: Igor A. Maznitsa 
// EMail : rrg@forth.org.ru
// Raydac Research Group (http://www.forth.org.ru/~rrg)
//============================================================
import java.util.*;  

public class BSGameSession 
{
	//====Game constatns===
    static final byte SHIPS_COUNT_1=4;		// Number of well-type ships
    static final byte SHIPS_COUNT_2=3;		// Number of two-funneled ships
    static final byte SHIPS_COUNT_3=2;		// Number of three-funneled ships
    static final byte SHIPS_COUNT_4=1;		// Number of four-funneled ships
    
    static final int SUMMARY_SHIPS_FIELDS=SHIPS_COUNT_1+SHIPS_COUNT_2*2+SHIPS_COUNT_3*3+SHIPS_COUNT_4*4;	// Summary ships fields
    static final int SUMMARY_SHIPS_COUNT=SHIPS_COUNT_1+SHIPS_COUNT_2+SHIPS_COUNT_3+SHIPS_COUNT_4;	// Summary ships number
	
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
	//=====================
	
	int playerid_1 = 0;	
	int playerid_2 = 0;

	long pl1_lpc = -1;
	long pl2_lpc = -1;
	
	boolean player1wait = true;
	boolean player2wait = true;

	long time_player1connectlost = 0;
	long time_player2connectlost = 0;
	
	int sessionID = 0;
	
	GameHTTPServer parent_server=null;

	boolean session_paused = false;
	
	boolean player1IsFirstMoving = false;
	static Random rnd;
	
	public boolean isSessionPause()
	{
		return session_paused;	
	}
	
	public void setTimeLostConnection(int userID)
	{
		if (playerid_1==userID) time_player1connectlost=System.currentTimeMillis();
		else
		if (playerid_2==userID) time_player2connectlost=System.currentTimeMillis();
	}
	
	public void setPlayerWaitMode(int player_id,boolean mode)
	{
		if (player_id==playerid_1) player1wait=mode;
		if (player_id==playerid_2) player2wait=mode;
	}

	public boolean isActive()
	{
		if ((playerid_1!=0)&&(playerid_2!=0)) return true; else return false;
	}
	
	public boolean isWait()
	{
		if ((playerid_1!=0)&&(playerid_2==0)) return true; else return false;
	}
	
	public boolean isEmpty()
	{
		if ((playerid_1==0)&&(playerid_2==0)) return true; else return false;
	}

	public boolean isLost()
	{
		boolean pl1_e = false;
		boolean pl2_e = false;
		if (playerid_1!=0)
		{
			if(parent_server.getPlayerStream(playerid_1)!=null) pl1_e = true;
			else
				if ((System.currentTimeMillis()-time_player1connectlost)>60000) pl1_e = false;
		}
		else
		{
			pl1_e = false;	
		}

		if (playerid_2!=0)
		{
			if(parent_server.getPlayerStream(playerid_2)!=null) pl2_e = true;
			else
				if ((System.currentTimeMillis()-time_player2connectlost)>60000) pl2_e = false;
		}
		else
		{
			pl2_e = false;	
		}
		
		if (pl2_e||pl1_e) return false; else return true;
		
	}
	
	public void activate(int owner_id)
	{
		System.out.println("Player "+owner_id+" activated session "+sessionID);

		session_paused = false;
		
		playerid_1 = owner_id;
		playerid_2 = 0;

		pl1_lpc = -1;
		pl2_lpc = -1;
		
		player1wait = true;
		player2wait = true;
		
		//========select the first moving player=======
		if ((rnd.nextDouble()*100)>50) 
			player1IsFirstMoving=true;
		else
			player1IsFirstMoving=false;
		//=============================================
	}
	
	public boolean join(int player_id,int sess)
	{
		System.out.println("Player "+player_id+" joined to session "+sessionID); 
		playerid_2 = player_id; 
		return sendDataPacket(player_id,new int[]{GC_OPPONENTJOIN,0,0,0},sess,true);
	}

	public boolean checkValidationPacketNumberForPlayer(int playerID,long packetnumber)
	{
			if(playerID==playerid_1) 
			{
								if (pl1_lpc<packetnumber)
								{
									if((packetnumber-pl1_lpc)>1)
									{
										System.out.println("Lost packet detected!");
									}
									
									pl1_lpc = packetnumber;
									return true;	
								}
								else
								return false;
			}
			else
			if (playerID==playerid_2)
			{
								if (pl2_lpc<packetnumber)
								{
									if((packetnumber-pl2_lpc)>1)
									{
										System.out.println("Lost packet detected!");
									}

									pl2_lpc = packetnumber;
									return true;	
								}
								else
								return false;
			 }
			else
			return false;
	}
	
	public boolean sendDataPacketForPlayerID(int playerID,int [] data_array)
	{
		int pl=0;
		if (playerID==playerid_1) pl = playerid_2; else pl = playerid_1;
		return sendDataPacket(pl,data_array,sessionID,true);  
	}
	
	public boolean checkValidationUser(int player_id)
	{
		if ((playerid_1==player_id)||(playerid_2==player_id)) return true ; else return false;
	}
	
	public boolean sendDataPacket(int sender_id,int [] data_array,int sess,boolean recurseenable)
	{
		if (isWait()) return false; 
		
		int reciever_id=0;

		synchronized(data_array)
		{
			if (sender_id == playerid_1) 
				reciever_id = playerid_2; 
			else 
				reciever_id = playerid_1;
			
			boolean modd=false;
			for(int la=0;la<20;la++)
			{
				if ((reciever_id==playerid_1)&&player1wait)modd=true;
				else
					if ((reciever_id==playerid_2)&&player2wait)modd=true;
				
				if (modd)
				{
					GameHTTPStream outstream = null;
					outstream = parent_server.getPlayerStream(reciever_id);
					
					if (outstream!=null)
					{
						if (outstream.sessionID!=this.sessionID) return false;
						for (int hh=0;hh<5;hh++)
						{
							if (outstream.sendPacket(data_array)) return true;
							try
							{
								Thread.sleep(300);
							}
							catch(InterruptedException ee)
							{ return false;}
						}
					}
				}
				try
				{
					Thread.sleep(500); 
				}
				catch(InterruptedException e)
				{
					return false;
				}
			}
			System.out.println("Can't send packet to player "+reciever_id);
		}
		
		// send to player command what opponent is lose
		if ((sess>=0)&&recurseenable)
			sendDataPacket(reciever_id,new int[]{GC_OPPONENTLOST,0,0,0},sessionID,false);
		
		this.close(); 
		
		return false;	
	}

	public BSGameSession(int session_id,GameHTTPServer parent)
	{
		this.parent_server = parent;  
		this.sessionID = session_id; 
		playerid_1 = 0;
		playerid_2 = 0;
		if (rnd==null) rnd = new Random(); 
	}
	
	public void closeSession()
	{
		if(playerid_1!=0) 
			sendDataPacketForPlayerID(playerid_1, new int[]{GC_SESSIONREMOVE,0,0,0}); 
		if(playerid_2!=0) 
			sendDataPacketForPlayerID(playerid_2, new int[]{GC_SESSIONREMOVE,0,0,0});
		close();
	}
	
	public void pause()
	{
		if (!this.session_paused)
		{
			if(playerid_1!=0) 
				sendDataPacketForPlayerID(playerid_1, new int[]{GC_SERVERPAUSE,0,0,0}); 
			if(playerid_2!=0) 
				sendDataPacketForPlayerID(playerid_2, new int[]{GC_SERVERPAUSE,0,0,0});
		}
		this.session_paused = true;
	}
	
	public void resume()
	{
		if (this.session_paused)
		{
			if(playerid_1!=0) 
				sendDataPacketForPlayerID(playerid_1, new int[]{GC_SERVERSTART,0,0,0}); 
			if(playerid_2!=0) 
				sendDataPacketForPlayerID(playerid_2, new int[]{GC_SERVERSTART,0,0,0});
		}
		this.session_paused = false;
	}
	
	public void close()
	{
		playerid_1 = 0;
		playerid_2 = 0;
		pl1_lpc = -1;
		pl2_lpc = -1;
		player1wait = true;
		player2wait = true;
	
		System.out.println("Session "+sessionID+" closed");
	}
	
	public int hashCode()
	{
		return sessionID;	
	}
	
}
