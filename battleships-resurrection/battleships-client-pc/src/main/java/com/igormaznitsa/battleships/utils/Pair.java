package com.igormaznitsa.battleships.utils;

import java.util.Objects;

@SuppressWarnings("unused")
public final class Pair<L, R> {
  private final L left;
  private final R right;

  private Pair(final L left, final R right) {
    this.left = left;
    this.right = right;
  }

  public static <L, R> Pair<L, R> of(final L left, final R right) {
    return new Pair<>(Objects.requireNonNull(left), Objects.requireNonNull(right));
  }

  public static <L, R> Pair<L, R> ofNullable(final L left, final R right) {
    return new Pair<>(left, right);
  }

  public L getLeft() {
    return left;
  }

  public R getRight() {
    return right;
  }
}
