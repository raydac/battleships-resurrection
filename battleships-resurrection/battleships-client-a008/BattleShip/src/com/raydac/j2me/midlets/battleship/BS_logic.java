//============================================================
// Author: Igor A. Maznitsa
// EMail : rrg@forth.org.ru
// Raydac Research Group (http://www.forth.org.ru/~rrg)
//============================================================
package com.raydac.j2me.midlets.battleship;

public class BS_logic
{
    // Game states
    public static final byte BS_FIRSTPAGE=0; // Output of the first page
    public static final byte BS_MENU=1;	   // Output of the menu page
    
    // Subpoint of MOVE state ==============================
    public static final byte BS_MOVE_OK = 0;			// All OK
    public static final byte BS_MOVE_GAMEPAUSE = 1;		// Pause of the game
    public static final byte BS_MOVE_GAMEEXIT = 2;		// Exit from the game
    public static final byte BS_MOVE_HIT = 3;			// Hit in ship
    public static final byte BS_MOVE_SHIPDESTRUCTION = 4;// Ship destruction
    public static final byte BS_MOVE_MISS = 5;			// Miss
    //======================================================
    
    // Constatnts for game field
    public static final byte BS_FIELD_EMPTY=0; // Empty field
    public static final byte BS_FIELD_SHIP=1;  // Ship field
    public static final byte BS_FIELD_HIT=2;   // Hit field
    public static final byte BS_FIELD_MISS=3;  // Miss field
    public static final byte BS_FIELD_SHIPDESTROY=4;// Hit in Ship
    
    // Game constants
    public static final byte GAMEFIELD_WIDTH=10;  // Width of the game field
    public static final byte GAMEFIELD_HEIGHT=10; // Height of the game field
    public static final byte SHIPS_COUNT_1=4;		// Number of well-type ships
    public static final byte SHIPS_COUNT_2=3;		// Number of two-funneled ships
    public static final byte SHIPS_COUNT_3=2;		// Number of three-funneled ships
    public static final byte SHIPS_COUNT_4=1;		// Number of four-funneled ships
    
    public static final int SUMMARY_SHIPS_FIELDS=SHIPS_COUNT_1+SHIPS_COUNT_2*2+SHIPS_COUNT_3*3+SHIPS_COUNT_4*4;	// Summary ships fields
    public static final int SUMMARY_SHIPS_COUNT=SHIPS_COUNT_1+SHIPS_COUNT_2+SHIPS_COUNT_3+SHIPS_COUNT_4;	// Summary ships number
    
    public static final byte DIRECTION_NORTHWARD = 0; //Northward
    public static final byte DIRECTION_SOUTHWARD = 1; //Southward
    public static final byte DIRECTION_WESTWARD = 2;  //Westward
    public static final byte DIRECTION_EASTWARD = 3;  //Eastward
    
    // Game variables
    public byte ourGameField [][] = null; // Our gamefield
    public byte opponentGameField [][] = null; // Opponent gamefield
    
    static java.util.Random rnd = null; // Randomize number generator
    protected static final int MAX_ATTEMPT = 75;
    
    public BS_logic()
    {
        if (rnd==null) rnd = new java.util.Random((System.currentTimeMillis() & 0xFFFF)<<6);
        
        ourGameField = new byte[GAMEFIELD_WIDTH][GAMEFIELD_HEIGHT];
        opponentGameField = new byte[GAMEFIELD_WIDTH][GAMEFIELD_HEIGHT];
    }
    
    public boolean isEndOfArrangement()
    {
        int lx,ly;
        int cnt = 0;
        for(lx=0;lx<GAMEFIELD_WIDTH;lx++)
            for(ly=0;ly<GAMEFIELD_HEIGHT;ly++)
            {
                if (ourGameField[lx][ly]==BS_FIELD_SHIP) cnt++;
            }
        
        if (cnt!=SUMMARY_SHIPS_FIELDS) return false; else return true;
    }
    
    protected void clearOurField()
    {
        int lx,ly;
        for(lx=0;lx<GAMEFIELD_WIDTH;lx++)
            for(ly=0;ly<GAMEFIELD_HEIGHT;ly++)
            {
                ourGameField[lx][ly]=BS_FIELD_EMPTY;
            }
    }
    
    protected void clearOpponentField()
    {
        int lx,ly;
        for(lx=0;lx<GAMEFIELD_WIDTH;lx++)
            for(ly=0;ly<GAMEFIELD_HEIGHT;ly++)
            {
                opponentGameField[lx][ly]=BS_FIELD_EMPTY;
            }
    }
    
    // Procedure of initialization of the game
    public void initGame()
    {
        // Clearing of the game fields
        clearOurField();
        clearOpponentField();
    }
    
    // Function of placing of the ships. If OK then TRUE else FALSE.
    public boolean placingShip(byte ship,byte start_x,byte start_y,byte direction)
    {
        byte dx=0;
        byte dy=0;
        
        int ll=0;
        
        int la=0;
        int lb=0;
        int la1=0;
        int lb1=0;
        
        switch (direction)
        {
            case DIRECTION_EASTWARD :{dx=1 ;dy=0 ;}; break;
            case DIRECTION_NORTHWARD:{dx=0 ;dy=-1 ;}; break;
            case DIRECTION_SOUTHWARD:{dx=0 ;dy=1 ;}; break;
            case DIRECTION_WESTWARD :{dx=-1 ;dy=0 ;}; break;
            default : {dx=0 ;dy=-1;}
        }
        
        // Check place for ship
        la = start_x;
        lb = start_y;
        
        for (ll=0;ll<ship;ll++)
        {
            if ((la>=GAMEFIELD_WIDTH)||(la<0)) return false;
            if ((lb>=GAMEFIELD_HEIGHT)||(lb<0)) return false;
            
            for (la1=(la-1);la1<=(la+1);la1++)
            {
                for (lb1=(lb-1);lb1<=(lb+1);lb1++)
                {
                    if ((lb1>=GAMEFIELD_HEIGHT)||(lb1<0)||(la1>=GAMEFIELD_WIDTH)||(la1<0)) continue;
                    if (ourGameField[la1][lb1]!=BS_FIELD_EMPTY) return false;
                }
            }
            la += dx;
            lb += dy;
        }
        
        // Place the Ship
        la = start_x;
        lb = start_y;
        for (ll=0;ll<ship;ll++)
        {
            ourGameField[la][lb]=BS_FIELD_SHIP;
            la += dx;
            lb += dy;
        }
        return true;
    }
    
    // Shot
    public byte shotResult(byte x,byte y)
    {
        if ((x>=GAMEFIELD_WIDTH)||(x<0)||(y>=GAMEFIELD_HEIGHT)||(y<0)) return BS_MOVE_MISS;
        
        int lx,ly;

        if (ourGameField[x][y]==BS_FIELD_SHIPDESTROY) return BS_MOVE_SHIPDESTRUCTION;
        if (ourGameField[x][y]==BS_FIELD_HIT) return BS_MOVE_HIT;    

        if (ourGameField[x][y]==BS_FIELD_SHIP)
        {
            for (lx=(x-1);lx<=(x+1);lx++)
                for (ly=(y-1);ly<=(y+1);ly++)
                {
                    if ((lx>=GAMEFIELD_WIDTH)||(lx<0)||(ly>=GAMEFIELD_HEIGHT)||(ly<0)) continue;
                    if (ourGameField[lx][ly]==BS_FIELD_SHIP) return BS_MOVE_HIT;
                }
            return BS_MOVE_SHIPDESTRUCTION;
        }
        else
            return BS_MOVE_MISS;
    }
    
    // Generation random number from 0 to num
    public byte getRnd(int num)
    {
        long llng;
        while(true)
        {
            int lint = java.lang.Math.abs(rnd.nextInt());
            llng = (long)(((long)lint*(long)num)>>31);
            if ((llng>=0)&&(llng<=num)) break;
        }
        return (byte)llng;
    }
    
    public void autoPlacingOurShips()
    {
        clearOurField();
        
        byte li=0;
        byte lx=0;
        byte ly=0;
        byte ld=0;
        
        int attempt=0;
        
        boolean put_flag = false;
        
        while(true)
        {
            clearOurField();
            
            //	Well-type ships
            for(li=0;li<SHIPS_COUNT_1;li++)
            {
                put_flag = false;
                attempt = 0;
                
                while(!put_flag)
                {
                    lx = getRnd(GAMEFIELD_WIDTH);
                    ly = getRnd(GAMEFIELD_HEIGHT);
                    if (placingShip((byte)1,lx,ly,ld))
                    {
                        put_flag=true;
                    }
                    attempt++;
                    if (attempt>MAX_ATTEMPT) break;
                }
                
                if (attempt>MAX_ATTEMPT) break;
            }
            
            if (attempt>MAX_ATTEMPT) continue;
            
            //	two-funneled ships
            for(li=0;li<SHIPS_COUNT_2;li++)
            {
                put_flag = false;
                attempt = 0;
                
                while(!put_flag)
                {
                    lx = getRnd(GAMEFIELD_WIDTH);
                    ly = getRnd(GAMEFIELD_HEIGHT);
                    ld = getRnd(DIRECTION_EASTWARD);
                    
                    for (byte ln = 0; ln<=4;ln++)
                    {
                        if (placingShip((byte)2,lx,ly,ld)) { put_flag=true; break;}
                        ld ++;
                        if (ld>DIRECTION_EASTWARD) ld=DIRECTION_NORTHWARD;
                    }
                    attempt ++;
                    if (attempt>MAX_ATTEMPT) break;
                }
                if (attempt>MAX_ATTEMPT) break;
            }
            if (attempt>MAX_ATTEMPT) continue;
            
            //	three-funneled ships
            for(li=0;li<SHIPS_COUNT_3;li++)
            {
                put_flag = false;
                attempt = 0;
                while(!put_flag)
                {
                    lx = getRnd(GAMEFIELD_WIDTH);
                    ly = getRnd(GAMEFIELD_HEIGHT);
                    ld = getRnd(DIRECTION_EASTWARD);
                    
                    for (byte ln = 0; ln<=4;ln++)
                    {
                        if (placingShip((byte)3,lx,ly,ld)) { put_flag=true; break;}
                        ld ++;
                        if (ld>DIRECTION_EASTWARD) ld=DIRECTION_NORTHWARD;
                    }
                    attempt ++;
                    if (attempt>MAX_ATTEMPT) break;
                }
                if (attempt>MAX_ATTEMPT) break;
            }
            if (attempt>MAX_ATTEMPT) continue;
            
            //	four-funneled ships
            for(li=0;li<SHIPS_COUNT_4;li++)
            {
                put_flag = false;
                attempt = 0;
                while(!put_flag)
                {
                    lx = getRnd(GAMEFIELD_WIDTH);
                    ly = getRnd(GAMEFIELD_HEIGHT);
                    ld = getRnd(DIRECTION_EASTWARD);
                    
                    for (byte ln = 0; ln<=4;ln++)
                    {
                        if (placingShip((byte)4,lx,ly,ld)) { put_flag=true; break;}
                        ld ++;
                        if (ld>DIRECTION_EASTWARD) ld=DIRECTION_NORTHWARD;
                    }
                    attempt ++;
                    if (attempt>MAX_ATTEMPT) break;
                }
                if (attempt>MAX_ATTEMPT) break;
            }
            if (attempt>MAX_ATTEMPT) continue; else break;
        }
    }
    
    public int getPercentOurDestroyed()
    {
        int fcount = 0;
        for(int ly=0;ly<GAMEFIELD_HEIGHT;ly++)
            for(int lx=0;lx<GAMEFIELD_WIDTH;lx++)
            {
                if ((ourGameField[lx][ly]==BS_FIELD_HIT)||(ourGameField[lx][ly]==BS_FIELD_SHIPDESTROY)) fcount+=1;
            }
        
        return  (fcount*1000/(SUMMARY_SHIPS_FIELDS))/10;
    }
    
    public int getPercentOpponentDestroyed()
    {
        int fcount = 0;
        for(int ly=0;ly<GAMEFIELD_HEIGHT;ly++)
            for(int lx=0;lx<GAMEFIELD_WIDTH;lx++)
            {
                if ((opponentGameField[lx][ly]==BS_FIELD_HIT)||(opponentGameField[lx][ly]==BS_FIELD_SHIPDESTROY)) fcount++;
            }
        
        return ((fcount*1000) /(SUMMARY_SHIPS_FIELDS))/10;
    }
    
    public byte opponentShot(byte x,byte y)
    {
        return 0;
    }
    
    protected int calcShipBit(byte x,byte y)
    {
        int lx0,ly0;
        int rslt=0;
        for(int lx=-1;lx<2;lx++)
            for(int ly=-1;ly<2;ly++)
            {
                lx0=x+lx;
                ly0=y+ly;
                if ((lx==0)&&(ly==0)) continue;
                if ((lx0<0)||(lx0>=GAMEFIELD_WIDTH)||(ly0<0)||(ly0>=GAMEFIELD_HEIGHT)) continue;
                if (opponentGameField[lx0][ly0]==BS_FIELD_HIT) rslt++;
            }
        return rslt;
    }
    
    // True if destroyed else False
    protected boolean isShipDestroed(byte x,byte y)
    {
        int lx0,ly0;
        boolean rslt=false;
        int dx=-1;
        int dy=-1;
        int dax=-1;
        int day=-1;
        
        for(int lx=-1;lx<2;lx++)
        {
            for(int ly=-1;ly<2;ly++)
            {
                lx0=x+lx;
                ly0=y+ly;
                if ((lx==0)&&(ly==0)) continue;
                if ((lx0<0)||(lx0>=GAMEFIELD_WIDTH)||(ly0<0)||(ly0>=GAMEFIELD_HEIGHT)) continue;
                if ((ourGameField[lx0][ly0]==BS_FIELD_HIT)||(ourGameField[lx0][ly0]==BS_FIELD_SHIP))
                {
                    rslt=true;
                    dx=lx;
                    dy=ly;
                    break;
                }
            }
            if (rslt) break;
        }
        
        if (!rslt) return true;
        
        dax=dx;
        day=dy;
        
        switch(dx)
        {
            case 1 : dax = -1;break;
            case -1 : dax = 1;break;
        }
        
        switch(dy)
        {
            case 1 : day = -1;break;
            case -1 : day = 1;break;
        }
        
        lx0=x;
        ly0=y;
        
        while(true)
        {
            if ((lx0<0)||(ly0<0)||(lx0>=GAMEFIELD_WIDTH)||(ly0>=GAMEFIELD_HEIGHT)) break;
            if ((ourGameField[lx0][ly0]==BS_FIELD_MISS)||(ourGameField[lx0][ly0]==BS_FIELD_EMPTY)) break;
            if (ourGameField[lx0][ly0]==BS_FIELD_SHIP) return false;
            lx0+=dx;
            ly0+=dy;
        }
        
        lx0=x;
        ly0=y;
        while(true)
        {
            if ((lx0<0)||(ly0<0)||(lx0>=GAMEFIELD_WIDTH)||(ly0>=GAMEFIELD_HEIGHT)) break;
            if ((ourGameField[lx0][ly0]==BS_FIELD_MISS)||(ourGameField[lx0][ly0]==BS_FIELD_EMPTY)) break;
            if (ourGameField[lx0][ly0]==BS_FIELD_SHIP) return false;
            lx0+=dax;
            ly0+=day;
        }
        return true;
    }
    
    protected void setShipBitDestroy(byte x,byte y)
    {
        int lx0,ly0;
        for(int lx=-1;lx<2;lx++)
            for(int ly=-1;ly<2;ly++)
            {
                lx0=x+lx;
                ly0=y+ly;
                if ((lx==0)&&(ly==0)) opponentGameField[lx0][ly0]=BS_FIELD_SHIPDESTROY;
                if ((lx0<0)||(lx0>=GAMEFIELD_WIDTH)||(ly0<0)||(ly0>=GAMEFIELD_HEIGHT)) continue;
                if ((opponentGameField[lx0][ly0]==BS_FIELD_HIT)||
                (opponentGameField[lx0][ly0]==BS_FIELD_SHIPDESTROY))
                    opponentGameField[lx0][ly0]=BS_FIELD_SHIPDESTROY;
                else opponentGameField[lx0][ly0]=BS_FIELD_MISS;
            }
    }
    
    public byte getShotResult(byte x,byte y)
    {
        byte lf = ourGameField[x][y];
        if (lf==BS_FIELD_SHIP){
            ourGameField[x][y]=BS_FIELD_HIT;
            if (isShipDestroed(x,y)) return BS_MOVE_SHIPDESTRUCTION;
            else return BS_MOVE_HIT;
        }
        ourGameField[x][y]=BS_FIELD_MISS;
        return BS_MOVE_MISS;
    }
    
    protected void setShipDestroyed(byte x,byte y)
    {
        int lx0,ly0;
        boolean rslt=false;
        int dx=-1;
        int dy=-1;
        int dax=-1;
        int day=-1;
        
        for(int lx=-1;lx<2;lx++)
        {
            for(int ly=-1;ly<2;ly++)
            {
                lx0=x+lx;
                ly0=y+ly;
                if ((lx==0)&&(ly==0)) continue;
                if ((lx0<0)||(lx0>=GAMEFIELD_WIDTH)||(ly0<0)||(ly0>=GAMEFIELD_HEIGHT)) continue;
                if ((opponentGameField[lx0][ly0]==BS_FIELD_HIT)||(opponentGameField[lx0][ly0]==BS_FIELD_SHIP))
                {
                    rslt=true;
                    dx=lx;
                    dy=ly;
                    break;
                }
            }
            if (rslt) break;
        }
        
        setShipBitDestroy(x,y);
        if (!rslt) return;
        
        dax=dx;
        day=dy;
        
        switch(dx)
        {
            case 1 : dax = -1;break;
            case -1 : dax = 1;break;
        }
        
        switch(dy)
        {
            case 1 : day = -1;break;
            case -1 : day = 1;break;
        }
        
        lx0=x;
        ly0=y;
        
        while(true)
        {
            if ((lx0<0)||(ly0<0)||(lx0>=GAMEFIELD_WIDTH)||(ly0>=GAMEFIELD_HEIGHT)) break;
            if ((opponentGameField[lx0][ly0]==BS_FIELD_MISS)||(opponentGameField[lx0][ly0]==BS_FIELD_EMPTY)) break;
            setShipBitDestroy((byte)lx0,(byte)ly0);
            lx0+=dx;
            ly0+=dy;
        }
        
        lx0=x;
        ly0=y;
        while(true)
        {
            if ((lx0<0)||(ly0<0)||(lx0>=GAMEFIELD_WIDTH)||(ly0>=GAMEFIELD_HEIGHT)) break;
            if ((opponentGameField[lx0][ly0]==BS_FIELD_MISS)||(opponentGameField[lx0][ly0]==BS_FIELD_EMPTY)) break;
            setShipBitDestroy((byte)lx0,(byte)ly0);
            lx0+=dax;
            ly0+=day;
        }
    }
    
    public void setShotResult(byte x,byte y,byte shot_status)
    {
        switch(shot_status)
        {
            case BS_MOVE_HIT : {
                opponentGameField[x][y] = BS_FIELD_HIT;
            };break;
            case BS_MOVE_MISS: {
                opponentGameField[x][y] = BS_FIELD_MISS;
            };break;
            case BS_MOVE_SHIPDESTRUCTION : {
                setShipDestroyed(x,y);
            };break;
        }
    }
    
    public int getStrikeCoord()
    {
        int lex=-1;
        int ley=-1;
        
        
        for(int ly=0;ly<GAMEFIELD_HEIGHT;ly++)
        {
            for(int lx=0;lx<GAMEFIELD_WIDTH;lx++)
            {
                byte lb = opponentGameField[lx][ly];
                switch(lb)
                {
                    case BS_FIELD_EMPTY : {
                        lex = lx;
                        ley = ly;
                    };break;
                    case BS_FIELD_HIT   : {
                        int dx=0;
                        int dy=0;
                        int dax=0;
                        int day=0;
                        boolean rslt = false;
                        
                        for(int ly0=-1;ly0<2;ly0++)
                        {
                            for(int lx0=-1;lx0<2;lx0++)
                            {
                                if (((lx0+ly0)!=1)&&((lx0+ly0)!=-1)) continue;
                                if ((lx0==0)&&(ly0==0)) continue;
                                dx = lx0+lx;
                                dy = ly0+ly;
                                if ((dx<0)||(dx>=GAMEFIELD_WIDTH)||(dy<0)||(dy>=GAMEFIELD_HEIGHT)) continue;
                                
                                switch(lx0)
                                {
                                    case -1 : dax = 1; break;
                                    case 1 : dax = -1; break;
                                    default : dax = 0;
                                }
                                switch(ly0)
                                {
                                    case -1 : day = 1; break;
                                    case 1 : day = -1; break;
                                    default : day = 0;
                                }
                                
                                if (opponentGameField[dx][dy]==BS_FIELD_HIT)
                                {
                                    dax += lx; day += ly;
                                    if ((dax<0)||(dax>=GAMEFIELD_WIDTH)||(day<0)||(day>=GAMEFIELD_HEIGHT))
                                    { rslt=true; break; }
                                    if (opponentGameField[dax][day]==BS_FIELD_EMPTY)
                                    {
                                        return (dax<<8)|day;
                                    } else
                                    {
                                        rslt=true;
                                        break;
                                    }
                                }
                            }
                            if (rslt) break;
                        }
                        if (rslt) continue;
                        
                        int lly0=getRnd(2)-1;
                        int llx0=getRnd(2)-1;
                        
                        for(int lycnt=0;lycnt<3;lycnt++)
                        {
                            for(int lxcnt=0;lxcnt<3;lxcnt++)
                            {
                                int lkk = llx0+lly0;
                                if ((lkk!=1)&&(lkk!=(-1))) continue;
                                if ((llx0==0)&&(lly0==0)) continue;
                                dx = llx0+lx;
                                dy = lly0+ly;
                                if ((dx<0)||(dx>=GAMEFIELD_WIDTH)||(dy<0)||(dy>=GAMEFIELD_HEIGHT)) continue;
                                if (opponentGameField[dx][dy]==BS_FIELD_EMPTY) return ((dx<<8)|dy);
                                llx0++;
                                if (llx0>1) llx0=-1;
                            }
                            lly0++;
                            if (lly0>1) lly0=-1;
                        }
                    };break;
                }
            }
        }
        
        for(int la=0;la<MAX_ATTEMPT;la++)
        {
            byte lx = getRnd(GAMEFIELD_WIDTH);
            byte ly = getRnd(GAMEFIELD_HEIGHT);
            
            if (opponentGameField[lx][ly]==BS_FIELD_EMPTY) return (lx<<8)|ly;
        }
        
        return (lex<<8)|ley;
    }
    
}
