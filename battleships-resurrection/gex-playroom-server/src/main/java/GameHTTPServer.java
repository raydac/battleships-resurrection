//============================================================
// Author: Igor A. Maznitsa 
// EMail : rrg@forth.org.ru
// Raydac Research Group (http://www.forth.org.ru/~rrg)
//============================================================
import java.io.*;
import java.net.*;
import java.util.*;

public class GameHTTPServer extends Thread
{
	ServerSocket ssckt;

	Hashtable sessions_table = null;
	Hashtable players_table = null;
	Hashtable users_table = null;

	boolean server_pause = false;
	boolean work_flag = true;

	SessionGarbageCollector sgc = null;
	
	boolean dyn_playerID = true;

	class SessionGarbageCollector extends Thread
	{
		int interval = 30;
		
		public void run()
		{
			while(work_flag)
			{
				Enumeration enm = sessions_table.elements(); 
				while(enm.hasMoreElements())
				{
					BSGameSession bsg = (BSGameSession) enm.nextElement(); 
					if (!bsg.isEmpty())
					{
						if (bsg.isLost())bsg.close(); 
					}
				}

				try
				{
					Thread.sleep(interval); 
				}
				catch(InterruptedException ee){return;}
			}
		}
		
		public SessionGarbageCollector(int interval_sec)
		{
			interval = interval_sec*1000; 
			this.setPriority(Thread.NORM_PRIORITY-1);  
			this.start(); 
		}
	}
	
	public GameHTTPStream getPlayerStream(int player_id)
	{
		return (GameHTTPStream)players_table.get(new Integer(player_id));
	}

	public synchronized void registerPlayerOutStream(GameHTTPStream ghs)
	{
		synchronized (players_table) 
		{
			players_table.put(new Integer(ghs.playerID),ghs);
		}
	}

	public synchronized void removePlayerOutStream(GameHTTPStream ghs)
	{
		synchronized (players_table)	
		{
			players_table.remove(new Integer(ghs.playerID));
		}
	}	
	
	public boolean IsServerPause()
	{
		return server_pause;	
	}
	
	public void serverPause()
	{
		server_pause = true;
		
		synchronized(sessions_table)
		{
			Enumeration enmm = sessions_table.elements(); 
			while(enmm.hasMoreElements())
			{
				BSGameSession bsg = (BSGameSession) enmm.nextElement();
				if (!bsg.isActive()&&(!bsg.isSessionPause()))
				{
					bsg.pause(); 	
				}
			}
		}
	}
	
	public BSGameSession getSession(int session_id)
	{
		return (BSGameSession)sessions_table.get(new Integer(session_id));
	}

	public boolean resumeSessionForUser(int playerID)
	{
		BSGameSession bsg = null;
		synchronized (players_table)
		{
			bsg = (BSGameSession)players_table.get(new Integer(playerID));
		}
		if (bsg==null) return false;
		bsg.resume(); 
		return true;
	}
	
	public boolean pauseSessionForUser(int playerID)
	{
		BSGameSession bsg = null;
		synchronized (players_table)
		{
			bsg = (BSGameSession)players_table.get(new Integer(playerID));
		}
		if (bsg==null) return false;
		bsg.pause();
		return true;
	}
	
	public boolean stopSessionForUser(int playerID)
	{
		BSGameSession bsg = null;
		synchronized (players_table)
		{
			bsg = (BSGameSession)players_table.get(new Integer(playerID));
		}
		if (bsg==null) return false;
		bsg.closeSession();
		return true;
	}

	public BSGameSession getWaitOrEmptySesssion()
	{
		synchronized(sessions_table)
		{
			BSGameSession emptys = null;
			BSGameSession current = null;
			Enumeration enm = sessions_table.elements(); 
			while(enm.hasMoreElements())
			{
				current = (BSGameSession) enm.nextElement();
				if (current.isEmpty()) emptys = current;  
				if (current.isWait()) return current;  
			}
			if (emptys == null) 
			{	
				System.out.println("Free sessions not found");
				return null;
			}
			else
			{
				return emptys;	
			}
		}
	}
	
	public void run()
	{
		try
		{
			while(work_flag)
			{
				Socket sckk = ssckt.accept();
				//System.out.println("Incomming connection");
				new GameHTTPStream(sckk,this);
			}
		}
		catch(IOException e){}
	}

	public void stopServer()
	{
		work_flag = false;
		try
		{
			ssckt.close();
		}
		catch(IOException e){}
		System.out.println("Server stoped");
	}

	public void addUserID(int userID)
	{
		players_table.put(new Integer(userID),null);   
	}

	public boolean setSessionToUser(int userID,BSGameSession bsg)
	{
		if (checkUserValidation(userID))
		{
			players_table.put(new Integer(userID),bsg);  
			return true;
		}
		else
			return false;
	}
	
	public boolean removeSessionFromUser(int userID)
	{
		if (checkUserValidation(userID))
		{
			players_table.put(new Integer(userID),null);  
			return true;
		}
		else
			return false;
	}
	
	public boolean checkUserValidation(int userID)
	{
		return players_table.containsKey(new Integer(userID));
	}
	
	public GameHTTPServer(int port,int sessions_number,boolean player_id_dyn)
	{
		try
		{
			ssckt = new ServerSocket(port); 
			System.out.println("Created server on "+port);
		}
		catch(IOException e)
		{
			System.out.println("Error of opening server socket "+port);
			return;
		}

		dyn_playerID = player_id_dyn; 
		
		players_table = new Hashtable(10);
		players_table.clear();  
		sessions_table = new Hashtable(sessions_number);
		sessions_table.clear();  
		users_table = new Hashtable(); 
		users_table.clear(); 
		for(int li=0;li<sessions_number;li++)
		{
			BSGameSession bsgs = new BSGameSession(li,this); 
			sessions_table.put( new Integer(bsgs.sessionID),bsgs);
		}
		System.out.println("Created session pool for "+sessions_number+" sessions"); 

		sgc = new SessionGarbageCollector(30); 
		System.out.println("Session garbage collector are started with interval 30 sec\r\n"); 
	}

	public void serverStart()
	{
		if (IsServerPause())
		{
			synchronized(sessions_table)
			{
				Enumeration enmm = sessions_table.elements(); 
				while(enmm.hasMoreElements())
				{
					BSGameSession bsg = (BSGameSession) enmm.nextElement();
					if (!bsg.isActive())
					{
						bsg.resume();
					}
				}
			}
		}
		
		if (this.isAlive()) return; 
		this.start();
	}
	
}
