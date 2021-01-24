/*
 *    Battleships PC client for GEX server
 *    Copyright (C) 2021 Igor Maznitsa
 *
 *    This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 */

package com.igormaznitsa.battleships;

import com.igormaznitsa.battleships.gui.BattleshipsFrame;
import com.igormaznitsa.battleships.gui.OpeningDialog;
import com.igormaznitsa.battleships.gui.StartData;
import com.igormaznitsa.battleships.opponent.AiBattleshipsSingleSessionBot;
import java.util.Optional;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Starter {

  public static void main(final String... args) {
    SwingUtilities.invokeLater(() -> {
      try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      } catch (Exception ex) {
        // ignoring
      }

      final StartData startData = StartData.newBuilder().build();
      final OpeningDialog openingDialog = new OpeningDialog(startData);
      openingDialog.setVisible(true);

      final Optional<StartData> selectedData = openingDialog.getResult();
      if (selectedData.isEmpty()) {
        System.exit(0);
      }

      final AiBattleshipsSingleSessionBot aiBot = new AiBattleshipsSingleSessionBot().start();
      new BattleshipsFrame(startData, aiBot, () -> {
        aiBot.dispose();
      }).setVisible(true);
    });
  }
}
