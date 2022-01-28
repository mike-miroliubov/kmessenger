package com.kite.kmessenger.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.reflect.Field


fun <T> getLogger(loggerName: String, loggerClass: Class<T>): T? {
    val logger: Logger = LoggerFactory.getLogger(loggerName)
    try {
        val loggerIntrospected: Class<out Logger> = logger.javaClass
        val fields: Array<Field> = loggerIntrospected.declaredFields
        for (i in fields.indices) {
            val fieldName: String = fields[i].getName()
            if (fieldName == "logger") {
                fields[i].setAccessible(true)
                return loggerClass.cast(fields[i].get(logger))
            }
        }
    } catch (e: Exception) {
        logger.error(e.message)
    }
    return null
}
