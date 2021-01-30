//============================================================
// Author: Christian Andersson
// EMail : christian@gamefederation.com
//============================================================

public class BattleshipsTester
{
    public static void main(String [] args)
    {
	try{
	    Class gameClass = Class.forName("GFfront");
	    IGFcontrol game = (IGFcontrol) gameClass.newInstance();
	    game.init(1,null);
	    game.assignServerPort(30000);
		game.addUser(1);
	    game.addUser(2);
	    game.start();
		}catch(Exception e)
	    {
			System.out.println("An exception was thrown: "+e.toString());
	    }
    }
}
