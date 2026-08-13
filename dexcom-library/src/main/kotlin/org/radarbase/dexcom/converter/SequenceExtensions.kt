package org.radarbase.dexcom.converter

import org.slf4j.LoggerFactory

val logger = LoggerFactory.getLogger("org.radarbase.oura.converter.SequenceExtensions")

internal fun <S, T> Sequence<T>.mapCatching(fn: (T) -> S): Sequence<Result<S>> = map { t ->
    runCatching {
        fn(t)
    }
}

internal fun <S, T> Sequence<T>.mapIndexedCatching(fn: (index: Int, T) -> S): Sequence<Result<S>> =
    mapIndexed { index, t ->
        runCatching {
            fn(index, t)
        }
    }
