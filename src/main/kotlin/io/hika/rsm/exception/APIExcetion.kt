package io.hika.rsm.exception

class IncompatibleVersionException(message: String) : RuntimeException(message)
class TerminateException(message: String) : RuntimeException(message)
class UnsupportedEventException(message: String) : RuntimeException(message)
