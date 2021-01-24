//============================================================
// Author: Igor A. Maznitsa 
// EMail : rrg@forth.org.ru
// Raydac Research Group (http://www.forth.org.ru/~rrg)
//============================================================
package com.raydac.j2me.midlets.battleship;

import java.io.*;

class BS_netException extends IOException
{
	public BS_netException(String str)
	{
		super(str);
	}
}