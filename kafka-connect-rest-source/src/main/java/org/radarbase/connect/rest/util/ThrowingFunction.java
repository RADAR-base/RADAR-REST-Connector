package org.radarbase.connect.rest.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
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

  static <T, R> Function<T, R> tryOrRethrow(ThrowingFunction<T, R> tryClause, BiFunction<T, Exception, RuntimeException> catchClause) {
    return t -> {
      try {
        return tryClause.apply(t);
      } catch (Exception e) {
        throw catchClause.apply(t, e);
      }
    };
  }

  static <T, R> Function<T, R> tryOrRethrow(ThrowingFunction<T, R> tryClause) {
    return tryOrRethrow(tryClause, (t, ex) -> {
      if (ex instanceof IOException) {
        throw new UncheckedIOException((IOException) ex);
      } else if (ex instanceof RuntimeException) {
        throw (RuntimeException) ex;
      } else {
        throw new RuntimeException(ex);
      }
    });
  }
}
