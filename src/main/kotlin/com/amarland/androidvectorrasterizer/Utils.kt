package com.amarland.androidvectorrasterizer

inline fun <T> Iterator<T>.firstOrThrow(
    throwable: () -> Throwable = { NoSuchElementException() }
) = takeIf { it.hasNext() }?.next() ?: throw throwable()
