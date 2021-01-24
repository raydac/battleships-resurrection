//============================================================
// Author: Igor A. Maznitsa 
// EMail : rrg@forth.org.ru
// Raydac Research Group (http://www.forth.org.ru/~rrg)
//============================================================
package com.raydac.j2me.midlets.battleship;

import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;
import java.io.*;
import java.util.*;

class BS_text
{
	String txt="";	
	int ix;
	int iy;
	int iw;
	int ih;
	Font fnt;
	boolean brd;	

	public BS_text(int x,int y,int w,int h,String text,Font font,boolean border)
	{
		brd = border;
		fnt = font;
		txt = text;
		ix = x;
		iy = y;
		iw =w;
		ih = h;
	}

	static public int calculateHeight(String str,int wd,Font fntn)
	{
		int cx=0;
		int wch;
		int fh = fntn.getHeight();
		int cy=fh;
		for (int li=0;li<str.length();li++)
		{
			char ch = str.charAt(li);
			if (ch=='\r')
			{
				cy+=fh;
				cx = 0;
				continue;
			}
			wch = fntn.charWidth(ch);
			if ((wd-cx)<wch)
			{
				cy+=fh;
				cx = 0;
			}
			cx+=wch;		
		}
		return cy;
	}

	static void drawText(Graphics g,Font fntn,int x,int y,int w,int h,String st)
	{
		g.setFont(fntn);
		int cx=0;
		int cy=0;
		int wch;
		int fh = fntn.getHeight();
		for (int li=0;li<st.length();li++)
		{
			char ch = st.charAt(li);
			if (ch=='\r')
			{
				cy+=fh;
				cx = 0;
				continue;
			}
			wch = fntn.charWidth(ch);
			if ((w-cx)<wch)
			{
				cy+=fh;
				cx = 0;
			}
			if (cy>=h) return;
			g.drawChar(ch,x+cx,y+cy,g.TOP|g.LEFT);
			cx+=wch;		
		}
	}

	public void paint(Graphics g)
	{
		g.setColor(0xFFFFFFFF);
		g.fillRect(ix,iy,iw,ih);
		g.setColor(0x00000000);
		if (brd)
		{
			g.drawRect(ix,iy,iw,ih);
		}
		drawText(g,fnt,ix,iy,iw,ih,txt);
	}

}