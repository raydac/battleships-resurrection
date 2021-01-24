//============================================================
// Author: Igor A. Maznitsa 
// EMail : rrg@forth.org.ru
// Raydac Research Group (http://www.forth.org.ru/~rrg)
//============================================================

public class BSSServer {

  public static void main(String[] args) {
    System.out.println("===http://www.rrg.da.ru====================================");
    System.out.println("|                                                         |");
    System.out.println("|         BATTLESHIP Internet Session Server              |");
    System.out.println("|                                                         |");
    System.out.println("|         Version   : 1.7b (10.04.2001)                   |");
    System.out.println("|         Author    : Igor A. Maznitsa (rrg@forth.org.ru) |");
    System.out.println("|                                                         |");
    System.out.println("===========================================================");

    GameHTTPServer serv = new GameHTTPServer(30000, 20, true);
    serv.serverStart();

    while (true) {
      try {
        Thread.sleep(10000);
      } catch (InterruptedException e) {
        serv.stopServer();
      }
    }

  }

}
