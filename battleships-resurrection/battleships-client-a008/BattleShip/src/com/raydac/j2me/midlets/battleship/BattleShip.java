//============================================================
// Author: Igor A. Maznitsa
// EMail : rrg@forth.org.ru
// Raydac Research Group (http://www.forth.org.ru/~rrg)
//============================================================
package com.raydac.j2me.midlets.battleship;


import com.gamefederation.playmaker.client.j2me.*;      
import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;
import javax.microedition.io.*;
import java.io.*;

public class BattleShip extends MIDlet
{
    private Display currentDisplay;
    private Canvas gameCanvas;
    
    protected void startApp()
    {
        currentDisplay.setCurrent(gameCanvas);
    }
    
    protected void pauseApp()
    {
        
    }
    
    protected void destroyApp(boolean unconditional)
    {
        
    }
    
    public BattleShip()
    {
        currentDisplay = Display.getDisplay(this);
        gameCanvas = new cBattleShip();
		((cBattleShip)gameCanvas).init(10,(int)System.currentTimeMillis(),"http://localhost:30000");
        ((cBattleShip)gameCanvas).start();
    }
    
}