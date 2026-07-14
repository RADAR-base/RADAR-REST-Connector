package org.radarbase.huawei.converter

import org.slf4j.LoggerFactory

val logger = LoggerFactory.getLogger("org.radarbase.huawei.converter.SequenceExtensions")

internal fun <S, T> Sequence<T>.mapCatching(fn: (T) -> S): Sequence<Result<S>> = map { t ->
    runCatching {
        fn(t)
    }
}
