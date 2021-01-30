//============================================================
// Author: Christian Andersson
// EMail : christian@gamefederation.com
//============================================================
import com.gamefederation.playmaker.session.IServiceAccess;
import com.gamefederation.playmaker.shared.exception.GFIllegalStateException;

public class GFfront implements IGFcontrol
{
    int port;
    GameHTTPServer serv;
    int state; 

    public GFfront()
    {
		state = NONEXISTENT;
    }
	    
    public void init(int configurationId, IServiceAccess services)
    {
		this.state = CREATING;
		// You won't be needing to do anything here right now
		// Will be used for calling on different services in the GF server 
    }

    public void assignServerPort(int port)
    {
		this.port = port;
		this.state = CREATED;
    }

    public void start() throws GFIllegalStateException
    {
		// first check that the server is in correct state to be started
		// a similar check should be done in every method
		if (state != CREATED)
		{
			throw new GFIllegalStateException();
	    }

		this.state = STARTING;

		// I don't really understand the second parameter here
		// The server should only start one server instance
		// The GF server takes care of starting several
		// instances if needed...or have i misunderstood its use
		this.serv = new GameHTTPServer(port,20,false);
		this.serv.serverStart();
		
		this.state = RUNNING;
    }

    public void stop()
    {
		this.serv.stopServer();
    }

    public void pause()
    {
	// Here the game should be paused so that no action can be taken
	// by the players
		this.serv.serverPause();
	}

    public boolean addUser(int clientId)
    {
		// Here you allocate resources for a player
		// and map the clientId to a certain player in the game

		// return true if you succeeded in adding the player
		// else return false if the player couldn't be added
		this.serv.addUserID(clientId);  
			return true;
    }

    public boolean deleteUser(int clientId)
    {
	// Here you end the game for the player with id clientId
	// Obvioulsy in battleships this will end the game

	// return true if you succeeded in deleting the player
	// else return false if the player couldn't be deleted
		return this.serv.stopSessionForUser(clientId);  
    }

    public boolean pauseUser(int clientId)
    {
	// Here you pause the game for the player with id clientId
	// Obvioulsy in battleships this will pause the entire game

	// return true if you succeeded in pausing the player
	// else return false if the player couldn't be paused
		return this.serv.pauseSessionForUser(clientId);
    }

     public boolean resumeUser(int clientId)
    {
	// Here you resume the game for the player with id clientId

	// return true if you succeeded in resuming the player
	// else return false if the player couldn't be resumed

		 return this.serv.resumeSessionForUser(clientId); 
    }

}
