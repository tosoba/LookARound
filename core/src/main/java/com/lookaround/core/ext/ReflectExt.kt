package com.lookaround.core.ext

inline fun <reified T> Any.getFieldValueByName(name: String): T? {
    val field = this::class.java.getDeclaredField(name)
    field.isAccessible = true
    return field.get(this) as? T
}
