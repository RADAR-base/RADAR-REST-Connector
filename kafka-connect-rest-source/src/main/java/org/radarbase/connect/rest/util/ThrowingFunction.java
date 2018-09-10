/*
 * Copyright 2018 The Hyve
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

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
