package org.radarbase.connect.rest;

import java.util.function.BiConsumer;
import java.util.function.Function;

@FunctionalInterface
public interface ThrowingFunction<T, R> {
  R apply(T value) throws Exception;

  static <T, R> Function<T, R> tryOrNull(ThrowingFunction<T, R> tryClause, BiConsumer<T, Exception> catchClause) {
    return t -> {
      try {
        return tryClause.apply(t);
      } catch (Exception e) {
        catchClause.accept(t, e);
        return null;
      }
    };
  }
}
