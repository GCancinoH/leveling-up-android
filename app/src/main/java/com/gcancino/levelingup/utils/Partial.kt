package com.gcancino.levelingup.utils

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

class Partial<T : Any>(private val klass: KClass<T>) {
    private val properties = mutableMapOf<String, Any?>()

    fun set(property: KProperty1<T, *>, value: Any?) {
        properties[property.name] = value
    }

    fun get(property: KProperty1<T, *>): Any? {
        return properties[property.name]
    }

    fun build(): T {
        val constructor = klass.primaryConstructor
            ?: throw IllegalArgumentException("Class must have a primary constructor")

        val args = constructor.parameters.map { param ->
            properties[param.name] ?: getDefaultValue(param.type.classifier as? KClass<*>)
        }.toTypedArray()

        return constructor.call(*args)
    }

    private fun getDefaultValue(type: KClass<*>?): Any? {
        return when (type) {
            String::class -> ""
            Int::class -> 0
            Long::class -> 0L
            Double::class -> 0.0
            Float::class -> 0.0f
            Boolean::class -> false
            else -> null
        }
    }
}

// Extension function for easier usage
inline fun <reified T : Any> partial(): Partial<T> = Partial(T::class)

// DSL-style builder
inline fun <reified T : Any> partial(builder: Partial<T>.() -> Unit): T {
    return partial<T>().apply(builder).build()
}

// Map-based constructor for object-literal-like syntax
inline fun <reified T : Any> partialOf(vararg pairs: Pair<String, Any?>): T {
    val partial = partial<T>()
    pairs.forEach { (key, value) ->
        partial.properties[key] = value
    }
    return partial.build()
}

// Extension to make properties accessible
val <T : Any> Partial<T>.properties: MutableMap<String, Any?>
    get() = this.javaClass.getDeclaredField("properties").let { field ->
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        field.get(this) as MutableMap<String, Any?>
    }