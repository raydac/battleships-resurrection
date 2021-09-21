/*
 *    Battleships PC client with GFX multi-player game support
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

package com.igormaznitsa.battleships.sound;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public enum Sound {
  AIR_CRASH1("AirCrash01.wav"),
  AIR_CRASH2("AirCrash03.wav"),
  AIRPLANE_IN("airplane in.wav"),
  AIRPLANE_OUT("AirplaneOut.wav"),
  ATTACK_HORN("Attack Horn.wav"),
  BUBBLES("Bubbles.wav"),
  DECK_CREAK("Deck Creak.wav"),
  DEFEAT_HORN("Defeat Horn.wav", true),
  DRENDOUT_FIRE("Drendout Fire.wav"),
  EXPLODE01("Explode01.wav"),
  EXPLODE03("explode03.wav"),
  GUARD_BOAT_FIRE("Guard Boat Fire.wav"),
  MENU_SCREEN_IN("Menu Screen In.wav"),
  MENU_SCREEN_OUT("Menu Screen Out.wav"),
  MENU_IN("MenuIn.wav"),
  MORSE("Morse.wav"),
  MORSE2("Morse2.wav"),
  MOUSE_CLICK("Mouse Click.wav"),
  MOUSE_FREE("Mouse Free.wav"),
  OUR_EXPLODE_ONLY("our explode only.wav"),
  OUR_WATER_SPLASH_ONLY("our water splash only.wav"),
  ROCKET_IN("rocket in.wav"),
  SEAGULL_01("Seagull 01.wav"),
  SEAGULL_02("Seagull 02.wav"),
  SEAGULL_03("Seagull 03.wav"),
  SUBMARINE_FIRE("Submarine Fire.wav"),
  TUNBLER_CRACK_AND_BELL("Tumbler Crack & Bell.wav"),
  TYPEWRITER("Typewriter.wav"),
  VICTORY_HORN("Victory Horn.wav", true),
  WATER_SPLASH01("WaterSplash01.wav"),
  WATER_SPLASH02("WaterSplash02.wav"),
  WAVES_LOOP("Waves Loop.wav"),
  SOS("sos.wav", true),
  WRONG_TUMBLER("WrongTumbler.wav");

  private final String resource;
  private final boolean lazy;
  private Optional<SoundClip> clip = Optional.empty();

  Sound(final String resource) {
    this(resource, false);
  }

  Sound(final String resource, final boolean lazy) {
    this.resource = resource;
    this.lazy = lazy;
  }

  public static void stopAll() {
    for (final Sound s : Sound.values()) {
      s.clip.ifPresent(SoundClip::stop);
    }
  }

  public synchronized void load(final boolean realLoadAllowed, final boolean forceLazy) {
    if (!this.lazy || forceLazy && this.clip.isEmpty()) {
      this.clip = realLoadAllowed ? Optional.of(new SoundClip(this.resource)) : Optional.empty();
    }
  }

  public synchronized void play() {
    this.clip.ifPresent(SoundClip::play);
  }

  public synchronized void playRepeat() {
    this.clip.ifPresent(c -> c.play(-1));
  }

  public synchronized void dispose() {
    this.clip.ifPresent(SoundClip::close);
    this.clip = Optional.empty();
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public synchronized boolean isPlaying() {
    return this.clip.map(SoundClip::isPlaying).orElse(false);
  }

  public void stop() {
    this.clip.ifPresent(SoundClip::stop);
  }
}
