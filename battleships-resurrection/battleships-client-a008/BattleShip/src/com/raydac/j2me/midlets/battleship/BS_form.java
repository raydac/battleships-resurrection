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

class BS_form
{
	Canvas parent;
	public final static byte MSG_INFO = 0;
	public final static byte MSG_ERROR = 1;
	public final static byte MSG_WARNING = 2;
	public final static byte MSG_QUESTION = 3;

	final static int SCREEN_WIDTH = 240;
	final static int SCREEN_HEIGHT = 236;

	BS_grbutton [] buttons;
	BS_text [] texts;
	byte buttonscount;
	byte textscount;

	Image []	number_img = null;
	Image []	alpha_img = null;

	final static int MAX_BUTTONS=15;
	final static int MAX_TEXTS=10;

	public boolean isPainted;
	static Image form_header=null;

	static Image iconWarning;
	static Image iconInfo;
	static Image iconError;
	static Image iconQuestion;

	static Image buttonOk;
	static Image buttonNo;
	static Image buttonYes;

	static int header_h;
	static Font header_font = Font.getFont(Font.FACE_SYSTEM,Font.STYLE_BOLD,Font.SIZE_SMALL);

	BS_buttonevent close_buttonclick;

	static int fnt_yoffst;
	BS_paintevent pevent;

	boolean havingHeader;
	Image dlgimage=null;

	int dialog_button_x;
	int dialog_button_y;
	
	int dialog_buttonno_x;
	int dialog_buttonno_y;

	byte message_type;

	private boolean dialog_ok_pressed;
	private boolean dialog_no_pressed;

	public boolean isModalWindowOut()
	{
		if (dlgimage!=null) return true; else return false; 	
	}
	
	public void changeButtonCoord(BS_grbutton b, int x,int y)
	{
		synchronized(b)
		{
			if (havingHeader)
			{
				b.iy=y+header_h;
				b.ix=x+1;
			}
		}
	}

	private boolean checkOKDialog(int x,int y)
	{
		int lx = x-dialog_button_x;
		int ly = y-dialog_button_y;

		if ((lx<0)||(ly<0)) return false;

		if ((lx<buttonOk.getWidth())&&(ly<buttonOk.getHeight())) return true; else return false;
	}

	private boolean checkNODialog(int x,int y)
	{
		int lx = x-dialog_buttonno_x;
		int ly = y-dialog_button_y;

		if ((lx<0)||(ly<0)) return false;

		if ((lx<buttonNo.getWidth())&&(ly<buttonNo.getHeight())) return true; else return false;
	}

	public void drawImage(Graphics g,int x,int y,Image img)
	{
		if (havingHeader)
		{
			x++;
			y+=header_h;
		}
		g.drawImage(img,x,y,Graphics.TOP|Graphics.LEFT);
	}

	public void drawNumber(Graphics g,int x,int y,int num,boolean percent)
	{
		if (havingHeader)
		{
			x++;
			y+=header_h;
		}

		String strnum = Integer.toString(num);
		int wdth = number_img[0].getWidth();
		for(int lk=0;lk<strnum.length();lk++)
		{
			int cur_sim = (int)strnum.charAt(lk)-0x30;
			g.drawImage(number_img[cur_sim],x,y,Graphics.TOP|Graphics.LEFT);
			x+=wdth;
		}

		if (percent)
		{
			g.drawImage(number_img[10],x,y,Graphics.TOP|Graphics.LEFT);			
		}
        	}

	public void drawCoord(Graphics g,int x,int y,int coordx,int coordy)
	{
		coordx++;
		if (havingHeader)
		{
			x++;
			y+=header_h;
		}

		int wdth = number_img[0].getWidth();
		g.drawImage(alpha_img[coordy],x,y,Graphics.TOP|Graphics.LEFT);
		x+=wdth;

		String strnum = Integer.toString(coordx);
		for(int lk=0;lk<strnum.length();lk++)
		{
			int cur_sim = (int)strnum.charAt(lk)-0x30;
			g.drawImage(number_img[cur_sim],x,y,Graphics.TOP|Graphics.LEFT);
			x+=wdth;
		}

        	}

	public void drawString(Graphics g,int x,int y,String str)
	{
		if (havingHeader)
		{
			x++;
			y+=header_h;
		}
		g.drawString(str,x,y,Graphics.TOP|Graphics.LEFT);
	}

	public void drawLine(Graphics g,int xs,int ys,int xe,int ye)
	{
		if (havingHeader)
		{
			xs++;
			ys+=header_h;
			xe++;
			ye+=header_h;
		}
		g.drawLine(xs,ys,xe,ye);
	}

	public void fillRect(Graphics g,int x,int y,int w,int h)
	{
		if (havingHeader)
		{
			x++;
			y+=header_h;
		}
		g.fillRect(x,y,w,h);
	}

	public void setPaintEvent(BS_paintevent paint_event)
	{
		pevent = paint_event;
	}

	public void clear()
	{
		synchronized (buttons)
		{
			for(int li=0;li<MAX_BUTTONS;li++)
			{
				buttons[li]=null;				
			}
			buttonscount=0;
		}

		synchronized (texts)
		{
			for(int li=0;li<MAX_TEXTS;li++)
			{
				texts[li]=null;				
			}
			textscount=0;
		}
		System.gc();
	}

	public void setFormType(boolean caption)
	{
		if (caption)
		{
			try 
			{
				if (form_header==null)
				{
					form_header = Image.createImage("/formheader.png");
					header_h = form_header.getHeight();
					fnt_yoffst = (header_h-header_font.getHeight())/2;
				}
			}	
			catch (IOException e)
			{
				System.out.println("Error of loading form header");
				header_h = 0;
			}
			havingHeader = true;			
		}
		else
		{
			havingHeader = false;
		}
	}

	public void paint(Graphics g)
	{

		if (dlgimage!=null)
		{
			int llx = (SCREEN_WIDTH - dlgimage.getWidth())/2;
			int lly = (SCREEN_HEIGHT - dlgimage.getHeight())/2;
			g.drawImage(dlgimage,llx,lly,g.TOP|g.LEFT);
			return;
		}

		if (!isPainted)
		{
			g.setColor(0xFFFFFFFF);
			g.fillRect(0,0,239,275);		

			if (havingHeader)
			{
				g.drawImage(form_header,0,0,Graphics.TOP|Graphics.LEFT);
			}

		}

		synchronized(buttons)
		{
			for(int li=0;li<textscount;li++)
			{
				texts[li].paint(g);
			}

			BS_grbutton cb;
			for(int li=0;li<buttonscount;li++)
			{
				cb = buttons[li];
				if (!isPainted) cb.paint(g);
				else if (cb.always_repaint) cb.paint(g);
			}
		}
		isPainted = true;

		
		if (pevent!=null) pevent.paint(g);

		if (havingHeader)
		{
			g.setColor(0x00000000);
			g.drawRect(0,0,SCREEN_WIDTH-1,SCREEN_HEIGHT-1);
		}
	}
	
	public void setChanges()
	{
		isPainted=false;
	}

	public void addButton(BS_grbutton button)
	{
		synchronized(buttons)
		{
			if (havingHeader)
			{
				button.iy+=header_h;
				button.ix++;
			}
			buttons[buttonscount]=button;
			buttonscount++;
		}
	}

	public void addText(BS_text textblock)
	{
		synchronized(texts)
		{
			if (havingHeader)
			{
				textblock.iy+=header_h;
				textblock.ix++;
			}
			texts[textscount] = textblock;
			textscount++;
		}
	}

	public BS_form(Canvas prnt,boolean having_header,BS_paintevent paint_event,BS_buttonevent clck)
	{
		parent = prnt;	
		pevent = paint_event;
		buttons = new BS_grbutton[15];
		texts    = new BS_text[10];
		close_buttonclick= clck;

		buttonscount = 0;
		textscount = 0;

		try 
		{
			if (form_header==null)
			{
				iconError = Image.createImage("/i_err.png");
				iconInfo = Image.createImage("/i_inf.png");
				iconWarning = Image.createImage("/i_wrn.png");
				iconQuestion = Image.createImage("/i_qst.png");

				buttonOk = Image.createImage("/b_ok.png");
				buttonNo = Image.createImage("/b_no.png");
				buttonYes = Image.createImage("/b_yes.png");
				form_header = Image.createImage("/formheader.png");
				header_h = form_header.getHeight();
				fnt_yoffst = (header_h-header_font.getHeight())/2;

				number_img = new Image [11];
				alpha_img = new Image[10];

				alpha_img[0] = Image.createImage("/lt_a.png");
				alpha_img[1] = Image.createImage("/lt_b.png");
				alpha_img[2] = Image.createImage("/lt_c.png");
				alpha_img[3] = Image.createImage("/lt_d.png");
				alpha_img[4] = Image.createImage("/lt_e.png");
				alpha_img[5] = Image.createImage("/lt_f.png");
				alpha_img[6] = Image.createImage("/lt_g.png");
				alpha_img[7] = Image.createImage("/lt_h.png");
				alpha_img[8] = Image.createImage("/lt_i.png");
				alpha_img[9] = Image.createImage("/lt_j.png");

	  			number_img [0] = Image.createImage("/lt_0.png");
	  			number_img [1] = Image.createImage("/lt_1.png");
	  			number_img [2] = Image.createImage("/lt_2.png");
	  			number_img [3] = Image.createImage("/lt_3.png");
	  			number_img [4] = Image.createImage("/lt_4.png");
	  			number_img [5] = Image.createImage("/lt_5.png");
			  	number_img [6] = Image.createImage("/lt_6.png");
	  			number_img [7] = Image.createImage("/lt_7.png");
	  			number_img [8] = Image.createImage("/lt_8.png");
	  			number_img [9] = Image.createImage("/lt_9.png");
	  			number_img [10] = Image.createImage("/lt_percent.png");
			}
		}
		catch (IOException e)
		{
			System.out.println("Error of loading form header");
			header_h = 0;
		}

		havingHeader = having_header;
		isPainted = false;
	}

	public boolean MessageDialog(String message,byte dialog)
	{
		message_type = dialog;
		int immwdth = (SCREEN_WIDTH*4)/5;

		int txtwidth = immwdth-50;
		int txtheight=BS_text.calculateHeight(message,txtwidth,Font.getDefaultFont());

		dlgimage = null;
		System.gc();

		int fnthght  = header_font.getHeight();

		dlgimage = Image.createImage(immwdth,51+fnthght+txtheight);
		Graphics dlgg = dlgimage.getGraphics();

		Image icon = null;
		String caption=null;

		switch (dialog)
		{
			case MSG_INFO : { caption="Information";icon=iconInfo;} break;
			case MSG_ERROR : { caption="Error";icon=iconError;} break;
			case MSG_WARNING: { caption="Warning";icon=iconWarning;} break;
			case MSG_QUESTION: { caption="Question";icon=iconQuestion;} break;
		}
		
		int fntoffst  = 2;

		dlgg.setColor(0xFFFFFFFF);
		dlgg.fillRect(0,0,dlgimage.getWidth(),dlgimage.getHeight());
		dlgg.setColor(0x00000000);
		dlgg.drawRect(0,0,dlgimage.getWidth()-1,dlgimage.getHeight()-1);
		dlgg.fillRect(0,0,dlgimage.getWidth(),fnthght+4);

		dlgg.setFont(header_font);
		dlgg.setColor(0xFFFFFFFF);
		dlgg.drawString(caption,10,fntoffst,Graphics.TOP|Graphics.LEFT);
		dlgg.drawImage(icon,10,fnthght+14,Graphics.TOP|Graphics.LEFT);

		dlgg.setColor(0x00000000);
		BS_text.drawText(dlgg,Font.getDefaultFont(),40,fnthght+14,txtwidth,txtheight,message);

		if (dialog==MSG_QUESTION)
		{
			int lbx=buttonOk.getWidth()+buttonNo.getWidth()+10;

			dialog_button_x=(dlgimage.getWidth()-lbx)/2;
			dialog_button_y=dlgimage.getHeight()-buttonOk.getHeight()-15;
			dialog_buttonno_x=dialog_button_x+buttonOk.getWidth()+10;

			dlgg.drawImage(buttonYes,dialog_button_x,dialog_button_y,Graphics.TOP|Graphics.LEFT);
		}
		else
		{
			dialog_button_x=(dlgimage.getWidth()-buttonOk.getWidth())/2;
			dialog_button_y=dlgimage.getHeight()-buttonOk.getHeight()-15;
			dlgg.drawImage(buttonOk,dialog_button_x,dialog_button_y,Graphics.TOP|Graphics.LEFT);
		}


		if (dialog==MSG_QUESTION)
		{
			dlgg.drawImage(buttonNo,dialog_buttonno_x,dialog_button_y,Graphics.TOP|Graphics.LEFT);			
		}

		dialog_button_x+=(SCREEN_WIDTH - dlgimage.getWidth())/2;
		dialog_button_y+=(SCREEN_HEIGHT - dlgimage.getHeight())/2;
		dialog_buttonno_x+=(SCREEN_WIDTH - dlgimage.getWidth())/2;

		setChanges();
		parent.repaint();

		dialog_ok_pressed=false;
		dialog_no_pressed=false;

		while((!dialog_ok_pressed)&&(!dialog_no_pressed)) 
		{
			try
			{
				Thread.sleep(100);
			}
			catch(InterruptedException e){};
		}

		setChanges();
		parent.repaint();

		if (dialog_ok_pressed) return true; else return false;
	}

	public void dragPen(int startx,int starty,int endx,int endy)
	{
		synchronized(buttons)
		{
			for(int li=0;li<buttonscount;li++)
			{
				BS_grbutton bsb = buttons[li];
				if (bsb.de==null) continue;
				if (bsb.checkCoord(startx,starty)&&bsb.checkCoord(endx,endy))
				{
					bsb.de.drag(startx-bsb.ix,starty-bsb.iy,endx-bsb.ix,endy-bsb.iy);
				 return; }
			}
		}
	}

	public void setCloseClickEvent(BS_buttonevent be)
	{
		close_buttonclick = be;
	}

	private boolean checkCloseButtonClick(int x,int y)
	{
		if ((y>0)&&(y<15))
			if ((x>224)&&(x<235)) return true; else return false;
		return false;
	}

	public void setHavingHeader(boolean isheader)
	{
		havingHeader = isheader;
	}

	public boolean clickPen(int x,int y)
	{
		if (dlgimage!=null)
		{
			if (checkOKDialog(x,y))
			{
				dlgimage = null;
				dialog_ok_pressed = true;
			}
			else
			if (message_type==MSG_QUESTION)
			{
				if (checkNODialog(x,y))
				{
					dlgimage = null;
					dialog_no_pressed = true;
				}
			}
			return true;
		}

		if (havingHeader)
		{

			if (close_buttonclick!=null)
			{
				if (checkCloseButtonClick(x,y)) 
				{
					close_buttonclick.buttonClick(x,y);
					return false;
				}
			}
		}

		synchronized(buttons)
		{
			for(int li=0;li<buttonscount;li++)
			{
				BS_grbutton bsb = buttons[li];
				if (bsb.be==null) continue;
				if (bsb.checkCoord(x,y))
				{
					if (bsb.be!=null) 
					{	
						if (bsb.visible) bsb.be.buttonClick(x-bsb.ix,y-bsb.iy);
					}
				 return true; }
			}
		}
		return false;
	}

}