//============================================================
// Author: Igor A. Maznitsa 
// EMail : rrg@forth.org.ru
// Raydac Research Group (http://www.forth.org.ru/~rrg)
//============================================================
package com.raydac.j2me.midlets.battleship;

import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;
import java.io.*;

class BS_grbutton
{
	Image butImage; // button image
	int ix; //x coord of the button
	int iy; //y coord of the button
	int ih; // height of the button
	int iw; // width of the button

	boolean visible;
	boolean always_repaint;

	BS_buttonevent be; // Click event for the button
	BS_dragevent de; // Dragevent for the button

	public void paint(Graphics g)
	{
		if (this.visible) g.drawImage(butImage,ix,iy,Graphics.TOP|Graphics.LEFT);
	}

	public void setAlwaysRepaint(boolean flag)
	{
		always_repaint = flag;
	}

	public void setVisible(boolean mode)
	{
		this.visible = mode;
	}

	public BS_grbutton (int x,int y,String image_name,BS_buttonevent bevent,BS_dragevent devent)
	{
		this.be=bevent;
		this.de=devent;
		this.visible = true;
		ix = x;
		iy = y;
		
		try
		{
			butImage = Image.createImage(image_name);
			ih = butImage.getHeight();
			iw = butImage.getWidth();
		}
		catch(IOException e)
		{
			System.out.println("Error of loading button image \""+image_name+"\"");
			butImage = Image.createImage(20,20);
			ih =19;
			iw = 19;
			butImage.getGraphics().setColor(0x00000000);
			butImage.getGraphics().drawChar('X',2,2,Graphics.TOP|Graphics.LEFT);
			butImage.getGraphics().drawRect(0,0,19,19);
		}
	}

	public void setClickEvent(BS_buttonevent e)
	{
		this.be = e;
	}

	public void setImage(Image img)
	{
		butImage = img;
	}

	public BS_grbutton (int x,int y,Image img,BS_buttonevent bevent,BS_dragevent devent)
	{
		be=bevent;
		de=devent;	
		ix = x;
		iy = y;
		this.visible = true;
		
		butImage = img;
		ih = butImage.getHeight();
		iw = butImage.getWidth();
	}

	public boolean checkCoord(int x,int y)
	{
		int lx=x-ix;
		int ly=y-iy;
		if ((lx<0)||(ly<0))return false;
		if ((lx<=iw)&&(ly<=ih)) return true; else return false;
	}
}