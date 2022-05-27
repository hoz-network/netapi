package net.hoz.netapi.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import org.slf4j.Logger

fun <T> Flow<T>.onErrorHandle(log: Logger): Flow<T> = catch { ex -> log.warn("Exception: ${ex.message}", ex) }