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

class cBattleShip extends Canvas implements Runnable, IComponentStateControl {
    int component_id = -1;
    int client_id = -1;
    String serverurl = null;

    boolean net_game;

    boolean moving_order = true;
    BS_form currentForm;
    BS_logic ourBSlogic;
    BS_logic opponentBSlogic;
    BS_net net_con;

    byte game_state;
    byte game_substate;
    byte buttonn;

    boolean opponentingame;

    BS_buttonevent select_be = null;

    int st_x;
    int st_y;
    boolean drag;

    int component_state;

    static final int GFC_NONE = 0;
    static final int GFC_INIT = 1;
    static final int GFC_START = 2;
    static final int GFC_PAUSE = 3;
    static final int GFC_STOP = 4;

    static final int harrowX = 150;

    static final byte GAME_FIRSTPAGE = 0;
    static final byte GAME_MENU = 1;
    static final byte GAME_GAME = 2;
    static final byte GAME_NETGAME = 3;
    static final byte GAME_HELP = 4;
    static final byte GAME_ABOUT = 5;
    static final byte GAME_PAUSE = 6;
    static final byte GAME_EXIT = 7;
    static final byte GAME_CANCEL = 8;
    static final byte GAME_ARRANGEMENT = 9;

    static final byte BUTTON_NONE = -1;
    static final byte BUTTON_OK = 0;
    static final byte BUTTON_CANCEL = 1;
    static final byte BUTTON_PAUSE = 2;
    static final byte BUTTON_AUTO = 3;
    static final byte BUTTON_CLEAR = 4;
    static final byte BUTTON_PLACE = 5;
    static final byte BUTTON_EXIT = 6;

    static final byte BUTTON_4TS = 7;
    static final byte BUTTON_3TS = 8;
    static final byte BUTTON_2TS = 9;
    static final byte BUTTON_1TS = 10;

    int number1TS;
    int number2TS;
    int number3TS;
    int number4TS;

    Image but1t;
    Image but2t;
    Image but3t;
    Image but4t;

    Image fiEMPTY;
    Image fiSHIP;
    Image fiMISS;
    Image fiHIT;
    Image fiDESTROY;
    Image fiBigEMPTY;
    Image fiBigSHIP;

    Image harrow;
    Image selectpanel;

    Image lost_screen;
    Image victory_screen;

    byte select_ship;
    byte oldselect_ship;

    Image bigOurFieldImage;
    Graphics bigOurFieldImageGraphics;

    Image bigOpponentFieldImage;
    Graphics bigOpponentFieldImageGraphics;

    Image mainMenuButtonGAME;
    Image mainMenuButtonNETGAME;
    Image mainMenuButtonHELP;
    Image mainMenuButtonABOUT;

    // Game icons
    Image giMoving;
    Image giWaitresult;
    Image giWaiting;
    Image giPause;
    Image giArrangement;
    Image giSPause;

    Image gpanel;
    Image buffer_image1;

    BS_grbutton gamefld_selectbutton;
    BS_grbutton gamefld_OurStatus;
    BS_grbutton gamefld_OurBigField;
    BS_grbutton gamefld_OpponentStatus;
    BS_grbutton gamefld_OpponentBigField;
    BS_grbutton gamefld_infopanel;

    BS_buttonevent close_buttonevent;

    BS_grbutton arrangement_field;

    Font gamefont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);

    int our_lastmovex;
    int our_lastmovey;
    int opponent_lastmovex;
    int opponent_lastmovey;
    int our_destroyed;
    int opponent_destroyed;

    boolean flag_select;

    int our_moving_x;
    int our_moving_y;

    int splace_x;
    int splace_y;
    int eplace_x;
    int eplace_y;

    protected boolean getExit() {
        if (currentForm.MessageDialog("Exit program?", BS_form.MSG_QUESTION)) {
            return true;
        } else {
            game_state = GAME_MENU;
            buttonn = BUTTON_NONE;
            game_substate = BUTTON_NONE;
            return false;
        }
    }

    private void outAbout() {
        game_substate = BUTTON_NONE;
        currentForm.clear();
        currentForm.addButton(new BS_grbutton(94, 176, "/b_ok.png", new BS_buttonevent() {
            public void buttonClick(int x, int y) {
                game_substate = BUTTON_OK;
            }
        }, null));
        currentForm.addButton(new BS_grbutton(0, 0, "/txt_about.png", null, null));

        currentForm.setChanges();
        repaint();

        while (true) {

            while (game_substate == BUTTON_NONE) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    game_substate = BUTTON_EXIT;
                    return;
                }
            }

            if (game_substate == BUTTON_EXIT) {
                if (getExit())
                    return;
                else
                    continue;
            }

            if (component_state == GFC_STOP)
                return;

            return;
        }
    }

    private void outHelp() {
        game_substate = BUTTON_NONE;
        currentForm.clear();
        currentForm.addText(
                new BS_text(10, 10, 218, 150, "In this version HELP not found.", Font.getDefaultFont(), false));
        currentForm.addButton(new BS_grbutton(94, 176, "/b_ok.png", new BS_buttonevent() {
            public void buttonClick(int x, int y) {
                game_substate = BUTTON_OK;
            }
        }, null));

        currentForm.setChanges();
        repaint();

        while (true) {
            while (game_substate == BUTTON_NONE) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    game_substate = BUTTON_EXIT;
                    return;
                }
            }

            if (game_substate == BUTTON_EXIT) {
                if (getExit())
                    return;
                else
                    continue;
            }

            if (component_state == GFC_STOP)
                return;

            return;
        }
    }

    private boolean ArrangementShips() throws BS_netException {
        // Arrangement of the Ships

        number1TS = BS_logic.SHIPS_COUNT_1;
        number2TS = BS_logic.SHIPS_COUNT_2;
        number3TS = BS_logic.SHIPS_COUNT_3;
        number4TS = BS_logic.SHIPS_COUNT_4;

        game_substate = BUTTON_NONE;
        buttonn = BUTTON_NONE;

        select_ship = 4;
        oldselect_ship = -4;

        buffer_image1 = Image.createImage(131, 131);
        Graphics buffer1_graphics = buffer_image1.getGraphics();

        fillFieldImage(buffer1_graphics, 14, 0, ourBSlogic.ourGameField);

        BS_grbutton arrangement_field = new BS_grbutton(5, 40, buffer_image1, null, new BS_dragevent() {
            public void drag(int startx, int starty, int endx, int endy) {
                splace_x = startx / 13;
                splace_y = starty / 13;
                eplace_x = endx / 13;
                eplace_y = endy / 13;
                game_substate = BUTTON_PLACE;
            }
        });
        arrangement_field.setAlwaysRepaint(true);

        currentForm.clear();
        currentForm.setPaintEvent(new BS_paintevent() {
            public void paint(Graphics g) {
                if (oldselect_ship == select_ship)
                    return;
                g.setColor(0xFFFFFFFF);
                currentForm.fillRect(g, 150, 40, 7, 106);
                currentForm.fillRect(g, 221, 40, 17, 106);
                int ly = 0;
                switch (select_ship) {
                    case 4:
                        ly = 49;
                        break;
                    case 3:
                        ly = 77;
                        break;
                    case 2:
                        ly = 105;
                        break;
                    case 1:
                        ly = 133;
                        break;
                }
                oldselect_ship = select_ship;
                currentForm.drawImage(g, harrowX, ly, harrow);

                g.setColor(0x00000000);
                g.setFont(Font.getDefaultFont());

                currentForm.drawNumber(g, 224, 43, number4TS, false);
                currentForm.drawNumber(g, 224, 72, number3TS, false);
                currentForm.drawNumber(g, 224, 99, number2TS, false);
                currentForm.drawNumber(g, 224, 127, number1TS, false);
            }
        });
        currentForm.addButton(new BS_grbutton(14, 14, "/aos.png", null, null));

        currentForm.addButton(arrangement_field);

        currentForm.addButton(new BS_grbutton(160, 40, but4t, new BS_buttonevent() {
            public void buttonClick(int x, int y) {
                buttonn = BUTTON_4TS;
            }
        }, null));
        currentForm.addButton(new BS_grbutton(160, 68, but3t, new BS_buttonevent() {
            public void buttonClick(int x, int y) {
                buttonn = BUTTON_3TS;
            }
        }, null));
        currentForm.addButton(new BS_grbutton(160, 96, but2t, new BS_buttonevent() {
            public void buttonClick(int x, int y) {
                buttonn = BUTTON_2TS;
            }
        }, null));
        currentForm.addButton(new BS_grbutton(160, 124, but1t, new BS_buttonevent() {
            public void buttonClick(int x, int y) {
                buttonn = BUTTON_1TS;
            }
        }, null));

        currentForm.addButton(new BS_grbutton(165, 152, "/b_auto.png", new BS_buttonevent() {
            public void buttonClick(int x, int y) {
                game_substate = BUTTON_AUTO;
            }
        }, null));
        currentForm.addButton(new BS_grbutton(165, 170, "/b_clear.png", new BS_buttonevent() {
            public void buttonClick(int x, int y) {
                game_substate = BUTTON_CLEAR;
            }
        }, null));

        currentForm.addButton(new BS_grbutton(64, 192, "/b_ok.png", new BS_buttonevent() {
            public void buttonClick(int x, int y) {
                game_substate = BUTTON_OK;
            }
        }, null));
        currentForm.addButton(new BS_grbutton(124, 192, "/b_cncl.png", new BS_buttonevent() {
            public void buttonClick(int x, int y) {
                game_substate = BUTTON_CANCEL;
            }
        }, null));

        currentForm.setChanges();
        repaint();

        opponentingame = false;

        arrangementcycle: {
            while (true) {
                game_substate = BUTTON_NONE;
                while ((game_substate == BUTTON_NONE) && (BS_net.getCmnd(net_con, 0) == BS_net.GC_NONE)) {
                    switch (buttonn) {
                        case BUTTON_4TS: {
                            buttonn = BUTTON_NONE;
                            select_ship = 4;
                            repaint();
                        }break;
                        case BUTTON_3TS: {
                            buttonn = BUTTON_NONE;
                            select_ship = 3;
                            repaint();
                        }break;
                        case BUTTON_2TS: {
                            buttonn = BUTTON_NONE;
                            select_ship = 2;
                            repaint();
                        }break;
                        case BUTTON_1TS: {
                            buttonn = BUTTON_NONE;
                            select_ship = 1;
                            repaint();
                        }break;
                    }

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        return false;
                    }
                }

                if (net_game) {
                    switch (BS_net.getCmnd(net_con, 0)) {
                        case BS_net.GC_NETERROR: {
                            currentForm.MessageDialog(BS_net.stringNetError, BS_form.MSG_ERROR);
                            return false;
                        }
                        case BS_net.GC_EXIT: {
                            currentForm.MessageDialog(BS_net.stringOpponentLeaved, BS_form.MSG_INFO);
                            return false;
                        }
                        case BS_net.GC_GAME:
                            opponentingame = true;
                            break;
                        case BS_net.GC_SESSIONREMOVE: {
                            currentForm.MessageDialog(BS_net.stringSessionRemoved, BS_form.MSG_INFO);
                            return false;
                        }
                    }
                    net_con.clearBuffer();
                    if (game_substate == BUTTON_NONE)
                        continue;
                }

                if (game_substate == BUTTON_CANCEL) {
                    if (currentForm.MessageDialog("Do you want leave the game?", BS_form.MSG_QUESTION))
                        return false;
                    oldselect_ship = -1;
                    continue;
                }

                if (game_substate == BUTTON_EXIT) {
                    if (getExit()) {
                        return false;
                    } else {
                        oldselect_ship = -1;
                        continue;
                    }
                }

                if (component_state == GFC_STOP)
                    return false;

                if (game_substate == BUTTON_PLACE) {
                    boolean ship_notfound = false;

                    byte drx = 0;
                    byte dry = 0;
                    byte drct = 0;

                    if (splace_x < eplace_x)
                        drx = -1;
                    else
                        drx = 1;
                    if (splace_y < eplace_y)
                        dry = -1;
                    else
                        dry = 1;

                    if (Math.abs(splace_x - eplace_x) < Math.abs(splace_y - eplace_y)) {
                        if (dry != 1)
                            drct = BS_logic.DIRECTION_SOUTHWARD;
                        else
                            drct = BS_logic.DIRECTION_NORTHWARD;
                    } else {
                        if (drx != 1)
                            drct = BS_logic.DIRECTION_EASTWARD;
                        else
                            drct = BS_logic.DIRECTION_WESTWARD;
                    }

                    byte shl = 0;

                    switch (select_ship) {
                        case 1: {
                            if (number1TS != 0)
                                shl = 1;
                            else
                                ship_notfound = true;
                        }
                            ;
                            break;
                        case 2: {
                            if (number2TS != 0)
                                shl = 2;
                            else
                                ship_notfound = true;
                        }
                            ;
                            break;
                        case 3: {
                            if (number3TS != 0)
                                shl = 3;
                            else
                                ship_notfound = true;
                        }
                            ;
                            break;
                        case 4: {
                            if (number4TS != 0)
                                shl = 4;
                            else
                                ship_notfound = true;
                        }
                            ;
                            break;
                    }
                    if (ship_notfound) {
                        oldselect_ship = -1;
                        currentForm.MessageDialog("Ships not found!", BS_form.MSG_ERROR);
                        continue;
                    }

                    if (ourBSlogic.placingShip(shl, (byte) splace_x, (byte) splace_y, drct)) {
                        fillFieldImage(buffer1_graphics, 14, 0, ourBSlogic.ourGameField);
                        oldselect_ship = -1;
                        switch (select_ship) {
                            case 1:
                                number1TS--;
                                break;
                            case 2:
                                number2TS--;
                                break;
                            case 3:
                                number3TS--;
                                break;
                            case 4:
                                number4TS--;
                                break;
                        }

                        repaint();
                    } else {
                        oldselect_ship = -1;
                        currentForm.MessageDialog("Can\'t place the ship!", BS_form.MSG_WARNING);
                    }
                    continue;
                }

                if (game_substate == BUTTON_CLEAR) {
                    ourBSlogic.clearOurField();
                    fillFieldImage(buffer1_graphics, 14, 0, ourBSlogic.ourGameField);

                    oldselect_ship = -1;

                    number1TS = BS_logic.SHIPS_COUNT_1;
                    number2TS = BS_logic.SHIPS_COUNT_2;
                    number3TS = BS_logic.SHIPS_COUNT_3;
                    number4TS = BS_logic.SHIPS_COUNT_4;

                    repaint();
                    continue;
                }

                if (game_substate == BUTTON_AUTO) {
                    ourBSlogic.autoPlacingOurShips();
                    byte[][] arr = ourBSlogic.ourGameField;

                    fillFieldImage(buffer1_graphics, 14, 0, ourBSlogic.ourGameField);

                    oldselect_ship = -1;

                    number1TS = 0;
                    number2TS = 0;
                    number3TS = 0;
                    number4TS = 0;

                    repaint();
                    continue;
                }
                if (game_substate == BUTTON_OK) {
                    if (ourBSlogic.isEndOfArrangement())
                        break arrangementcycle;
                    else {
                        oldselect_ship = -1;
                        buttonn = BUTTON_NONE;
                        currentForm.MessageDialog("Arrangement is\r not ended!", currentForm.MSG_ERROR);
                    }
                }
            }
        }
        buffer1_graphics = null;
        buffer_image1 = null;

        return true;
    }

    private boolean outNetGame() {
        opponentingame = false;

        try {
            net_con = new BS_net(serverurl, client_id);
        } catch (IOException r) {
            net_con = null;
            currentForm.MessageDialog(BS_net.stringServerNotFound, BS_form.MSG_ERROR);
            return true;
        }

        boolean new_session = false;

        // Get session number
        waitsession: {
            while (true) {
                while (BS_net.getCmnd(net_con, 0) == BS_net.GC_NONE) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ee) {
                        return true;
                    }
                }

                switch (BS_net.getCmnd(net_con, 0)) {
                    case BS_net.GC_OPPONENTLOST:
                        continue;

                    case BS_net.GC_SERVERPAUSE: {
                        currentForm.MessageDialog("Server stopped!", BS_form.MSG_INFO);
                        return false;
                    }
                    case BS_net.GC_NEWSESSION: {
                        moving_order = true;
                        new_session = true;
                        net_con.setSessionID(BS_net.getCmnd(net_con, 1));
                        if (BS_net.getCmnd(net_con, 2) != 0)
                            moving_order = true;
                        else
                            moving_order = false;
                        break waitsession;
                    }
                    case BS_net.GC_JOINTOSESSION: {
                        moving_order = false;
                        net_con.setSessionID(BS_net.getCmnd(net_con, 1));
                        if (BS_net.getCmnd(net_con, 2) != 0)
                            moving_order = true;
                        else
                            moving_order = false;
                        break waitsession;
                    }
                    default: {
                        net_con.clearBuffer();
                    }
                }
            }
        }
        net_con.clearBuffer();

        try {
            if (new_session) {
                // Output screen about the waiting of an opponent

                currentForm.clear();
                currentForm.addText(
                        new BS_text(10, 10, 218, 140, "Wait of opponent connection!", Font.getDefaultFont(), false));
                currentForm.addButton(new BS_grbutton(94, 176, "/b_cncl.png", new BS_buttonevent() {
                    public void buttonClick(int x, int y) {
                        game_substate = BUTTON_CANCEL;
                    }
                }, null));

                currentForm.setChanges();
                repaint();

                game_substate = BUTTON_NONE;

                waitjoin: {
                    while (true) {
                        game_substate = BUTTON_NONE;
                        net_con.clearBuffer();

                        while ((game_substate == BUTTON_NONE) && (BS_net.getCmnd(net_con, 0) == BS_net.GC_NONE)) {
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException ee) {
                                return true;
                            }
                        }

                        switch (BS_net.getCmnd(net_con, 0)) {
                            case BS_net.GC_OPPONENTJOIN:
                                break waitjoin;
                            case BS_net.GC_EXIT: {
                                currentForm.MessageDialog(BS_net.stringSessionRemoved, BS_form.MSG_INFO);
                                return false;
                            }
                            case BS_net.GC_SESSIONREMOVE: {
                                currentForm.MessageDialog(BS_net.stringSessionRemoved, BS_form.MSG_INFO);
                                return false;
                            }
                        }

                        net_con.clearBuffer();

                        if (game_substate == BUTTON_EXIT)
                            if (getExit()) {
                                return true;
                            }
                        if (game_substate == BUTTON_CANCEL) {
                            return false;
                        }
                        if (component_state == GFC_STOP)
                            return true;
                    }
                }
            }
            game_substate = BUTTON_NONE;
            buttonn = BUTTON_NONE;
            return outGame();

        } catch (BS_netException nex) {
            currentForm.MessageDialog(nex.getMessage(), BS_form.MSG_ERROR);
            return false;
        } finally {
            if (net_con != null) {
                net_con.sessionClose();
                net_con.close();
            }
            net_con = null;

            currentForm.setPaintEvent(null);
        }
    }

    private void setOurGameStatus(Image statusimage) {
        gamefld_OurStatus.setImage(statusimage);
    }

    private void setOpponentGameStatus(Image statusimage) {
        gamefld_OpponentStatus.setImage(statusimage);
    }

    private void initGamePanel() {
        fillFieldImage2(bigOurFieldImageGraphics, 15, 10, ourBSlogic.ourGameField);
        fillFieldImage2(bigOpponentFieldImageGraphics, 15, 10, ourBSlogic.opponentGameField);

        buttonn = BUTTON_NONE;
        currentForm.clear();

        gamefld_selectbutton = new BS_grbutton(0, 3, selectpanel, null, null);
        gamefld_OurBigField = new BS_grbutton(5, 45, bigOurFieldImage, null, null);

        gamefld_OpponentBigField = new BS_grbutton(5, 45, bigOpponentFieldImage, new BS_buttonevent() {
            public void buttonClick(int sx, int sy) {
                sx = sx - 10;
                our_moving_x = sx / 14;
                our_moving_y = sy / 14;
            }
        }, null);

        gamefld_OurBigField.setAlwaysRepaint(true);
        gamefld_OpponentBigField.setAlwaysRepaint(true);

        BS_grbutton gamefld_infopanel = new BS_grbutton(165, 95, gpanel, null, null);
        gamefld_infopanel.setAlwaysRepaint(true);

        currentForm.addButton(gamefld_infopanel);

        gamefld_OurStatus = new BS_grbutton(165, 35, giWaiting, null, null);
        gamefld_OpponentStatus = new BS_grbutton(165, 35, giWaiting, null, null);
        gamefld_OurStatus.setAlwaysRepaint(true);
        gamefld_OpponentStatus.setAlwaysRepaint(true);

        currentForm.addButton(gamefld_OurStatus);
        currentForm.addButton(gamefld_OpponentStatus);

        currentForm.addButton(new BS_grbutton(170, 180, "/b_pause.png", new BS_buttonevent() {
            public void buttonClick(int sx, int sy) {
                game_state = GAME_PAUSE;
            }
        }, null));

        currentForm.addButton(new BS_grbutton(170, 197, "/b_exit.png", new BS_buttonevent() {
            public void buttonClick(int sx, int sy) {
                game_state = GAME_CANCEL;
            }
        }, null));

        currentForm.addButton(gamefld_selectbutton);
        currentForm.addButton(gamefld_OurBigField);
        currentForm.addButton(gamefld_OpponentBigField);

        if (select_be == null)
            select_be = new BS_buttonevent() {
                public void buttonClick(int x, int y) {
                    if (x > 120) {
                        flag_select = false;
                        gamefld_OurBigField.setVisible(true);
                        gamefld_OpponentBigField.setVisible(false);

                        gamefld_OpponentStatus.setVisible(true);
                        gamefld_OurStatus.setVisible(false);
                    } else {
                        flag_select = true;
                        gamefld_OurBigField.setVisible(false);
                        gamefld_OpponentBigField.setVisible(true);

                        gamefld_OpponentStatus.setVisible(false);
                        gamefld_OurStatus.setVisible(true);
                    }
                    repaint();
                }
            };

        currentForm.setPaintEvent(new BS_paintevent() {
            public void paint(Graphics g) {
                g.setFont(gamefont);

                if (flag_select) {
                    // Opponent field active
                    g.setColor(0xFFFFFFFF);
                    currentForm.drawLine(g, 6, 25, 118, 25);
                    g.setColor(0x000000000);
                    currentForm.drawLine(g, 119, 25, 232, 25);

                    // Info out
                    int dig_num = 2;
                    if (our_lastmovex == 9)
                        dig_num = 3;
                    if (our_lastmovex >= 0)
                        currentForm.drawCoord(g, 165 + (60 - 10 * dig_num) / 2, 110, our_lastmovex, our_lastmovey);
                    dig_num = 2;
                    if (opponent_destroyed > 9)
                        dig_num = 3;
                    currentForm.drawNumber(g, 165 + (60 - dig_num * 10) / 2, 150, opponent_destroyed, true);
                } else {
                    // Our field active
                    g.setColor(0xFFFFFFFF);
                    currentForm.drawLine(g, 119, 25, 232, 25);

                    g.setColor(0x00000000);
                    currentForm.drawLine(g, 6, 25, 118, 25);

                    // Info out
                    int dig_num = 2;
                    if (opponent_lastmovex == 9)
                        dig_num = 3;
                    if (opponent_lastmovex >= 0)
                        currentForm.drawCoord(g, 165 + (60 - 10 * dig_num) / 2, 110, opponent_lastmovex,
                                opponent_lastmovey);
                    dig_num = 2;
                    if (our_destroyed > 9)
                        dig_num = 3;
                    currentForm.drawNumber(g, 165 + (60 - 10 * dig_num) / 2, 150, our_destroyed, true);
                }
            }
        });
        gamefld_selectbutton.setClickEvent(select_be);
        currentForm.setChanges();
    }

    private boolean outMovingConfirmationDialog(int move_x, int move_y) {
        String str = ShotToString(move_x, move_y);
        return currentForm.MessageDialog("Your moving is ".concat(str).concat(" ?"), BS_form.MSG_QUESTION);
    }

    private void outRepeatedShotDialog() {
        currentForm.MessageDialog("   Repeated shot!", BS_form.MSG_ERROR);
    }

    private boolean outEndGameDialog() {
        return currentForm.MessageDialog("  End the game?", BS_form.MSG_QUESTION);
    }

    private boolean outGame() throws BS_netException {
        System.gc();

        int game_end_result = -1;

        game_state = GAME_GAME;
        flag_select = true;

        ourBSlogic = new BS_logic();
        ourBSlogic.initGame();

        boolean opponentIsPause = false;
        boolean serverIsPause = false;

        opponentBSlogic = new BS_logic();
        opponentBSlogic.initGame();
        if (!net_game)
            opponentBSlogic.autoPlacingOurShips();

        // Arrangement of the Ships
        if (!ArrangementShips())
            return false;

        initGamePanel();

        our_lastmovex = -1;
        our_lastmovey = -1;

        opponent_lastmovex = -1;
        opponent_lastmovey = -1;

        our_destroyed = 0;
        opponent_destroyed = 0;

        setOurGameStatus(giWaiting);

        // Wait incomming opponent in game mode
        if (net_game) {
            net_con.sendDataPacket(BS_net.GC_GAME, 0, 0, 0);

            setOpponentGameStatus(giArrangement);
            setOurGameStatus(giWaiting);
            select_be.buttonClick(0, 0);

            game_state = GAME_GAME;

            while (!opponentingame) {
                while ((BS_net.getCmnd(net_con, 0) == BS_net.GC_NONE) && (game_state == GAME_GAME)) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ee) {
                        return true;
                    }
                }

                switch (BS_net.getCmnd(net_con, 0)) {
                    case BS_net.GC_GAME:
                        opponentingame = true;
                        continue;
                    case BS_net.GC_NETERROR: {
                        currentForm.MessageDialog(BS_net.stringNetError, BS_form.MSG_ERROR);
                        return false;
                    }
                    case BS_net.GC_EXIT: {
                        currentForm.MessageDialog(BS_net.stringOpponentLeaved, BS_form.MSG_INFO);
                        return false;
                    }
                    case BS_net.GC_SESSIONREMOVE: {
                        currentForm.MessageDialog(BS_net.stringSessionRemoved, BS_form.MSG_INFO);
                        return false;
                    }
                }

                net_con.clearBuffer();

                if (game_state == GAME_EXIT) {
                    if (getExit()) {
                        currentForm.setPaintEvent(null);
                        return true;
                    }

                    game_state = GAME_GAME;
                    continue;
                }

                if (component_state == GFC_STOP) {
                    currentForm.setPaintEvent(null);
                    return true;
                }

                if (game_state == GAME_CANCEL) {
                    if (outEndGameDialog()) {
                        currentForm.setPaintEvent(null);
                        return false;
                    }
                    game_state = GAME_GAME;
                    continue;
                }
            }
        }

        if (!net_game) {
            if (ourBSlogic.getRnd(100) > 50) {
                moving_order = true;
                setOurGameStatus(giMoving);
                setOpponentGameStatus(giWaiting);
            } else {
                moving_order = false;
                setOurGameStatus(giWaiting);
                setOpponentGameStatus(giMoving);
            }
        }

        select_be.buttonClick(0, 0);

        byte moving_result;
        byte move_x = 0;
        byte move_y = 0;

        while (true) {
            if (moving_order) {
                // Our moving
                our_moving_x = -1;
                setOurGameStatus(giMoving);
                setOpponentGameStatus(giWaiting);
                repaint();

                int old_cmpstate = GFC_NONE;

                while (true) {
                    while ((our_moving_x < 0) && (game_state == GAME_GAME)
                            && (net_con.getCmnd(net_con, 0) == BS_net.GC_NONE)) {
                        if (component_state != old_cmpstate) {
                            switch (component_state) {
                                case GFC_STOP:
                                    return true;
                                case GFC_PAUSE: {
                                    setOurGameStatus(giSPause);
                                    setOpponentGameStatus(giSPause);
                                    repaint();
                                }
                                    ;
                                    break;
                                case GFC_START: {
                                    setOurGameStatus(giMoving);
                                    setOpponentGameStatus(giWaiting);
                                    repaint();
                                }
                                    ;
                                    break;
                            }
                            old_cmpstate = component_state;
                        }

                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            return true;
                        }
                    }

                    if (net_game) {
                        if (BS_net.getCmnd(net_con, 0) != BS_net.GC_NONE) {
                            switch (BS_net.getCmnd(net_con, 0)) {
                                case BS_net.GC_GAME: {
                                    opponentIsPause = false;
                                    setOpponentGameStatus(giWaiting);
                                    repaint();
                                }break;
                                case BS_net.GC_NETERROR: {
                                    currentForm.MessageDialog(BS_net.stringNetError, BS_form.MSG_ERROR);
                                    return false;
                                }
                                case BS_net.GC_EXIT: {
                                    currentForm.MessageDialog(BS_net.stringOpponentLeaved, BS_form.MSG_INFO);
                                    return false;
                                }
                                case BS_net.GC_PAUSE: {
                                    opponentIsPause = true;
                                    setOpponentGameStatus(giPause);
                                    repaint();
                                }break;
                                case BS_net.GC_SERVERPAUSE: {
                                    serverIsPause = true;
                                    setOpponentGameStatus(giSPause);
                                    setOurGameStatus(giSPause);
                                }break;
                                case BS_net.GC_SERVERSTART: {
                                    serverIsPause = false;
                                    if (opponentIsPause) {
                                        setOpponentGameStatus(giPause);
                                        setOurGameStatus(giMoving);
                                    } else {
                                        setOpponentGameStatus(giWaiting);
                                        setOurGameStatus(giMoving);
                                    }
                                }break;
                                case BS_net.GC_SESSIONREMOVE: {
                                    currentForm.MessageDialog(BS_net.stringSessionRemoved, BS_form.MSG_INFO);
                                    return false;
                                }
                            }
                            net_con.clearBuffer();

                            if ((game_state == GAME_GAME) && (our_moving_x < 0))
                                continue;
                        }
                    }

                    if (game_state == GAME_PAUSE) {
                        if (net_game)
                            net_con.sendDataPacket(BS_net.GC_PAUSE, 0, 0, 0);
                        currentForm.MessageDialog("PAUSE", BS_form.MSG_INFO);
                        if (net_game) {
                            if (BS_net.getCmnd(net_con, 0) != BS_net.GC_EXIT) {
                                net_con.clearBuffer();
                                net_con.sendDataPacket(BS_net.GC_GAME, 0, 0, 0);
                            }
                        }
                        game_state = GAME_GAME;
                        continue;
                    }

                    if (game_state == GAME_EXIT) {
                        if (getExit()) {
                            currentForm.setPaintEvent(null);
                            return true;
                        }

                        game_state = GAME_GAME;
                        continue;
                    }

                    if (component_state == GFC_STOP) {
                        currentForm.setPaintEvent(null);
                        return true;
                    }

                    if (game_state == GAME_CANCEL) {
                        if (outEndGameDialog()) {
                            currentForm.setPaintEvent(null);
                            return false;
                        }
                        game_state = GAME_GAME;
                        continue;
                    }

                    if (our_moving_x < 0)
                        continue;

                    if (serverIsPause) {
                        currentForm.MessageDialog("Game server in pause!", BS_form.MSG_WARNING);
                        our_moving_x = -1;
                        continue;
                    }

                    if (opponentIsPause) {
                        currentForm.MessageDialog(" Opponent in pause!", BS_form.MSG_WARNING);
                        our_moving_x = -1;
                        continue;
                    }

                    if (component_state == GFC_PAUSE)
                        currentForm.MessageDialog(" Server in pause!", BS_form.MSG_WARNING);

                    if (ourBSlogic.opponentGameField[our_moving_x][our_moving_y] != BS_logic.BS_FIELD_EMPTY) {
                        outRepeatedShotDialog();
                        our_moving_x = -1;
                        continue;
                    }

                    move_x = (byte) our_moving_x;
                    move_y = (byte) our_moving_y;

                    our_lastmovex = our_moving_x;
                    our_lastmovey = our_moving_y;

                    if (outMovingConfirmationDialog(move_x, move_y))
                        break;
                    else
                        our_moving_x = -1;
                }

                if (net_game) {

                    net_con.sendDataPacket(BS_net.GC_GAMEMOVE, move_x, move_y, 0);

                    setOurGameStatus(giWaitresult);
                    repaint();

                    moving_result = -1;
                    for (int latt = 0; latt < 6000; latt++) {
                        if (BS_net.getCmnd(net_con, 0) == BS_net.GC_GAMERESULT) {
                            moving_result = (byte) BS_net.getCmnd(net_con, 1);
                            net_con.clearBuffer();
                            break;
                        } else {
                            net_con.clearBuffer();
                        }
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException dd) {
                            return true;
                        }
                    }
                    if (moving_result < 0) {
                        currentForm.MessageDialog(BS_net.stringOpponentLost, BS_form.MSG_ERROR);
                        return false;
                    }

                } else
                    moving_result = opponentBSlogic.getShotResult(move_x, move_y);

                ourBSlogic.setShotResult(move_x, move_y, moving_result);

                switch (moving_result) {
                    case BS_logic.BS_MOVE_HIT:
                        setFieldState2(bigOpponentFieldImageGraphics, move_x, move_y, 15, 10, BS_logic.BS_FIELD_HIT);
                        break;
                    case BS_logic.BS_MOVE_SHIPDESTRUCTION:
                        fillFieldImage2(bigOpponentFieldImageGraphics, 15, 10, ourBSlogic.opponentGameField);
                        break;
                    case BS_logic.BS_MOVE_MISS:
                        setFieldState2(bigOpponentFieldImageGraphics, move_x, move_y, 15, 10, BS_logic.BS_FIELD_MISS);
                        break;
                }
            } else {
                // Opponent moving
                setOurGameStatus(giWaiting);
                setOpponentGameStatus(giMoving);
                repaint();

                if (!net_game) {
                    int hod = opponentBSlogic.getStrikeCoord();

                    move_x = (byte) (hod >> 8);
                    move_y = (byte) (hod & 0xFF);

                    opponent_lastmovex = move_x;
                    opponent_lastmovey = move_y;
                } else {
                    int old_cstate = GFC_NONE;

                    hhh: {
                        while (true) {
                            game_state = GAME_GAME;
                            while ((BS_net.getCmnd(net_con, 0) == BS_net.GC_NONE) && (game_state == GAME_GAME)) {
                                try {
                                    Thread.sleep(50);
                                } catch (InterruptedException ee) {
                                    return true;
                                }
                            }

                            if (old_cstate != component_state) {
                                switch (component_state) {
                                    case GFC_START: {
                                        setOurGameStatus(giWaiting);
                                        setOpponentGameStatus(giMoving);
                                        repaint();
                                    }break;
                                    case GFC_PAUSE: {
                                        setOurGameStatus(giSPause);
                                        setOpponentGameStatus(giSPause);
                                        repaint();
                                    }break;
                                    case GFC_STOP:
                                        return true;

                                }

                                old_cstate = component_state;
                            }

                            if (net_game) {
                                switch (BS_net.getCmnd(net_con, 0)) {
                                    case BS_net.GC_NETERROR: {
                                        currentForm.MessageDialog(BS_net.stringNetError, BS_form.MSG_ERROR);
                                        return false;
                                    }
                                    case BS_net.GC_EXIT: {
                                        currentForm.MessageDialog(BS_net.stringOpponentLeaved, BS_form.MSG_INFO);
                                        return false;
                                    }
                                    case BS_net.GC_GAME: {
                                        opponentIsPause = false;
                                        setOpponentGameStatus(giMoving);
                                        repaint();
                                    }break;
                                    case BS_net.GC_PAUSE: {
                                        opponentIsPause = true;
                                        setOpponentGameStatus(giPause);
                                        repaint();
                                    }break;
                                    case BS_net.GC_SERVERSTART: {
                                        serverIsPause = false;
                                        if (opponentIsPause) {
                                            setOurGameStatus(giWaiting);
                                            setOpponentGameStatus(giPause);
                                        } else {
                                            setOurGameStatus(giWaiting);
                                            setOpponentGameStatus(giMoving);
                                        }
                                    }break;
                                    case BS_net.GC_SERVERPAUSE: {
                                        serverIsPause = true;
                                        setOurGameStatus(giSPause);
                                        setOpponentGameStatus(giSPause);
                                    }break;
                                    case BS_net.GC_SESSIONREMOVE: {
                                        currentForm.MessageDialog(BS_net.stringSessionRemoved, BS_form.MSG_INFO);
                                        return false;
                                    }
                                }

                                if ((game_state == GAME_GAME) && (BS_net.getCmnd(net_con, 0) == BS_net.GC_NONE))
                                    continue;

                                if (BS_net.getCmnd(net_con, 0) == BS_net.GC_GAMEMOVE) {
                                    move_x = (byte) BS_net.getCmnd(net_con, 1);
                                    move_y = (byte) BS_net.getCmnd(net_con, 2);
                                    net_con.clearBuffer();
                                    break hhh;
                                } else
                                    net_con.clearBuffer();

                                if (game_state == GAME_GAME)
                                    continue;
                            }

                            switch (game_state) {
                                case GAME_CANCEL: {
                                    if (outEndGameDialog()) {
                                        currentForm.setPaintEvent(null);
                                        return false;
                                    }
                                }break;
                                case GAME_EXIT: {
                                    if (getExit()) {
                                        currentForm.setPaintEvent(null);
                                        return true;
                                    }
                                }break;
                                case GAME_PAUSE: {

                                    if (net_game)
                                        net_con.sendDataPacket(BS_net.GC_PAUSE, 0, 0, 0);
                                    currentForm.MessageDialog("   PAUSE!", BS_form.MSG_INFO);
                                    if (net_game)
                                        net_con.sendDataPacket(BS_net.GC_GAME, 0, 0, 0);
                                    game_state = GAME_GAME;
                                }break;
                            }
                        }
                    }
                    opponent_lastmovex = move_x;
                    opponent_lastmovey = move_y;
                }
                moving_result = ourBSlogic.getShotResult(move_x, move_y);
                if (net_game) {
                    // System.out.println("Result for strike "+move_x+","+move_y);
                    net_con.sendDataPacket(BS_net.GC_GAMERESULT, moving_result, 0, 0);
                } else
                    opponentBSlogic.setShotResult(move_x, move_y, moving_result);

                switch (moving_result) {
                    case BS_logic.BS_MOVE_HIT:
                        setFieldState2(bigOurFieldImageGraphics, move_x, move_y, 15, 10, BS_logic.BS_FIELD_HIT);
                        break;
                    case BS_logic.BS_MOVE_SHIPDESTRUCTION:
                        setFieldState2(bigOurFieldImageGraphics, move_x, move_y, 15, 10, BS_logic.BS_FIELD_HIT);
                        break;
                    case BS_logic.BS_MOVE_MISS:
                        setFieldState2(bigOurFieldImageGraphics, move_x, move_y, 15, 10, BS_logic.BS_FIELD_MISS);
                        break;
                }
            }

            our_destroyed = ourBSlogic.getPercentOurDestroyed();
            if (our_destroyed >= 100) {
                repaint();
                currentForm.setPaintEvent(null);

                if (net_con != null) {
                    net_con.sessionClose();
                    net_con.close();
                    net_con = null;
                }

                outFullScreenButton(lost_screen, 500);
                return false;
            }

            opponent_destroyed = ourBSlogic.getPercentOpponentDestroyed();

            if (opponent_destroyed >= 100) {
                repaint();
                currentForm.setPaintEvent(null);

                if (net_con != null) {
                    net_con.sessionClose();
                    net_con.close();
                    net_con = null;
                }

                outFullScreenButton(victory_screen, 500);
                return false;
            }

            boolean moving_order_changed = false;
            if ((moving_result != BS_logic.BS_MOVE_HIT) && (moving_result != BS_logic.BS_MOVE_SHIPDESTRUCTION)) {
                moving_order = !moving_order;
                moving_order_changed = true;
            }
            repaint();

            if (!net_game) {
                if (!(moving_order && (!moving_order_changed))) {
                    try {
                        Thread.sleep(700);
                    } catch (InterruptedException e) {
                        return true;
                    }
                }
            }
        }

    }

    private String ShotToString(int x, int y) {
        String ch = (char) ('A' + y) + Integer.toString(x + 1);
        return ch;
    }

    private void setFieldState2(Graphics gr, int x, int y, int cell, int xoffst, byte state) {
        x = x * (cell - 1) + xoffst;
        y = y * (cell - 1);

        synchronized (gr) {
            switch (state) {
                case BS_logic.BS_FIELD_EMPTY:
                    gr.drawImage(fiEMPTY, x, y, Graphics.TOP | Graphics.LEFT);
                    break;
                case BS_logic.BS_FIELD_SHIP:
                    gr.drawImage(fiSHIP, x, y, Graphics.TOP | Graphics.LEFT);
                    break;
                case BS_logic.BS_FIELD_MISS:
                    gr.drawImage(fiMISS, x, y, Graphics.TOP | Graphics.LEFT);
                    break;
                case BS_logic.BS_FIELD_HIT:
                    gr.drawImage(fiHIT, x, y, Graphics.TOP | Graphics.LEFT);
                    break;
                case BS_logic.BS_FIELD_SHIPDESTROY:
                    gr.drawImage(fiDESTROY, x, y, Graphics.TOP | Graphics.LEFT);
                    break;
            }
        }
    }

    private void setFieldState(Graphics gr, int x, int y, int cell, int xoffst, byte state) {
        x = x * (cell - 1) + xoffst;
        y = y * (cell - 1);

        synchronized (gr) {
            switch (state) {
                case BS_logic.BS_FIELD_EMPTY:
                    gr.drawImage(fiBigEMPTY, x, y, Graphics.TOP | Graphics.LEFT);
                    break;
                case BS_logic.BS_FIELD_SHIP:
                    gr.drawImage(fiBigSHIP, x, y, Graphics.TOP | Graphics.LEFT);
                    break;
            }
        }
    }

    private void fillFieldImage(Graphics gr, int cell, int xoffst, byte[][] arr) {
        for (int lyy = 0; lyy < 10; lyy++) {
            for (int lxx = 0; lxx < 10; lxx++) {
                setFieldState(gr, lxx, lyy, cell, xoffst, arr[lxx][lyy]);
            }
        }
    }

    private void fillFieldImage2(Graphics gr, int cell, int xoffst, byte[][] arr) {
        for (int lyy = 0; lyy < 10; lyy++) {
            for (int lxx = 0; lxx < 10; lxx++) {
                setFieldState2(gr, lxx, lyy, cell, xoffst, arr[lxx][lyy]);
            }
        }
    }

    BS_buttonevent fullscreenevent = null;

    public void outFullScreenButton(Image image, int delay) {
        game_state = GAME_FIRSTPAGE;
        currentForm.setHavingHeader(false);
        currentForm.clear();
        currentForm.addButton(new BS_grbutton(0, 0, image, fullscreenevent, null));
        currentForm.setChanges();
        repaint();

        int li = 0;
        while ((game_state == GAME_FIRSTPAGE) && (li < delay)) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                return;
            }
            li++;
        }
        currentForm.setHavingHeader(true);

    }

    public void run() {
        // Init section
        fullscreenevent = new BS_buttonevent() {
            public void buttonClick(int x, int y) {
                game_state = GAME_MENU;
            }
        };

        close_buttonevent = new BS_buttonevent() {
            public void buttonClick(int x, int y) {
                game_state = GAME_EXIT;
                game_substate = BUTTON_EXIT;
                buttonn = BUTTON_EXIT;
            }
        };

        currentForm = new BS_form(this, false, null, close_buttonevent);

        Image fldb;
        try {
            fldb = Image.createImage("/fldb.png");
        } catch (IOException e) {
            return;
        }

        bigOpponentFieldImage = Image.createImage(151, 151);
        bigOpponentFieldImageGraphics = bigOpponentFieldImage.getGraphics();
        bigOpponentFieldImageGraphics.drawImage(fldb, 0, 0, Graphics.TOP | Graphics.LEFT);

        bigOurFieldImage = Image.createImage(151, 151);
        bigOurFieldImageGraphics = bigOurFieldImage.getGraphics();
        bigOurFieldImageGraphics.drawImage(fldb, 0, 0, Graphics.TOP | Graphics.LEFT);

        fldb = null;
        System.gc();

        // Main
        /*
         * try { Image foreimage = Image.createImage("/bs.png");
         * outFullScreenButton(foreimage,100); foreimage = null; } catch(IOException
         * ee){}
         */
        while (true) {

            if (game_state == GAME_EXIT) {
                if (getExit()) {
                    currentForm.clear();
                    currentForm.setPaintEvent(null);
                    currentForm.setCloseClickEvent(null);
                    currentForm = null;
                    repaint();
                    System.gc();
                    return;
                }
            }

            if (component_state == GFC_STOP) {
                currentForm.clear();
                currentForm.setPaintEvent(null);
                currentForm.setCloseClickEvent(null);
                currentForm = null;
                repaint();
                System.gc();
                return;
            }

            game_state = GAME_MENU;
            currentForm.clear();
            currentForm.setFormType(true);
            currentForm.addButton(new BS_grbutton(19, 28, mainMenuButtonGAME, new BS_buttonevent() {
                public void buttonClick(int x, int y) {
                    game_state = GAME_GAME;
                }
            }, null));
            currentForm.addButton(new BS_grbutton(19, 70, mainMenuButtonNETGAME, new BS_buttonevent() {
                public void buttonClick(int x, int y) {
                    game_state = GAME_NETGAME;
                }
            }, null));
            currentForm.addButton(new BS_grbutton(19, 112, mainMenuButtonHELP, new BS_buttonevent() {
                public void buttonClick(int x, int y) {
                    game_state = GAME_HELP;
                }
            }, null));
            currentForm.addButton(new BS_grbutton(19, 154, mainMenuButtonABOUT, new BS_buttonevent() {
                public void buttonClick(int x, int y) {
                    game_state = GAME_ABOUT;
                }
            }, null));
            currentForm.setChanges();
            repaint();
            while (game_state == GAME_MENU) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    return;
                }
            }

            currentForm.clear();

            switch (game_state) {
                case GAME_GAME: {
                    net_game = false;
                    try {
                        outGame();
                    } catch (BS_netException dd) {
                    }
                }
                    ;
                    break;
                case GAME_NETGAME: {
                    net_game = true;
                    outNetGame();
                }
                    ;
                    break;
                case GAME_HELP:
                    outHelp();
                    break;
                case GAME_ABOUT:
                    outAbout();
                    break;
            }
            System.gc();
        }
    }

    public void paint(Graphics g) {
        if (currentForm != null)
            currentForm.paint(g);
    }

    protected void pointerPressed(int x, int y) {
        if (currentForm != null) {
            currentForm.clickPen(x, y);
            st_x = x;
            st_y = y;
        }
    }

    protected void pointerReleased(int x, int y) {
        if (currentForm != null) {
            if (drag) {
                drag = false;
                currentForm.dragPen(st_x, st_y, x, y);
            }
        }
    }

    protected void pointerDragged(int x, int y) {
        if (currentForm != null) {
            drag = true;
        }
    }

    public cBattleShip() {
        component_state = GFC_NONE;
    }

    // Part for Game Federation platform

    /**
     * Starts the component
     */
    public void start() {
        switch (component_state) {
            case GFC_INIT: {
                component_state = GFC_START;
                Thread it = new Thread(this);
                it.start();
            }
                ;
                break;
            case GFC_PAUSE: {
                component_state = GFC_START;
            }
                ;
                break;
        }
    }

    /**
     * Pauses the component
     */
    public void pause() {
        component_state = GFC_PAUSE;
    }

    /**
     * Stops the component
     */
    public void stop() {
        switch (component_state) {
            case GFC_START: {
                component_state = GFC_STOP;
            }
                ;
                break;
            case GFC_PAUSE: {
                component_state = GFC_STOP;
            }
                ;
                break;
        }
    }

    /**
     * Initializes the component
     *
     * @param componentId A unique identifier of the component instance.
     * @param clientId    A unique identifier of the client running this component.
     * @param server      A host:port combination pointing at the server.
     */
    public void init(int componentId, int clientId, String server) {
        component_state = GFC_INIT;

        component_id = componentId;
        client_id = clientId;
        serverurl = server;

        Image fldb;
        try {
            but1t = Image.createImage("/b_1t.png");
            but2t = Image.createImage("/b_2t.png");
            but3t = Image.createImage("/b_3t.png");
            but4t = Image.createImage("/b_4t.png");

            lost_screen = Image.createImage("/lost.png");
            victory_screen = Image.createImage("/victory.png");
            fiEMPTY = Image.createImage("/fiempty.png");
            fiSHIP = Image.createImage("/fiship.png");
            fiHIT = Image.createImage("/fihit.png");
            fiDESTROY = Image.createImage("/fidestroy.png");
            fiMISS = Image.createImage("/fimiss.png");

            fiBigEMPTY = Image.createImage("/fibigempty.png");
            fiBigSHIP = Image.createImage("/fibigship.png");

            giMoving = Image.createImage("/gi_moving.png");
            giWaitresult = Image.createImage("/gi_network.png");
            giWaiting = Image.createImage("/gi_wait.png");
            giPause = Image.createImage("/gi_pause.png");
            giSPause = Image.createImage("/gi_spause.png");
            giArrangement = Image.createImage("/gi_arngmnt.png");

            gpanel = Image.createImage("/gpanel.png");

            mainMenuButtonGAME = Image.createImage("/b_game.png");
            mainMenuButtonNETGAME = Image.createImage("/b_ngame.png");
            mainMenuButtonHELP = Image.createImage("/b_help.png");
            mainMenuButtonABOUT = Image.createImage("/b_about.png");

            harrow = Image.createImage("/harrow.png");
            selectpanel = Image.createImage("/selpanel.png");

        } catch (IOException e) {
            System.out.println("Error of loading game images!");
            return;
        }
    }

}
