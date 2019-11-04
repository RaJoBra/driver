package de.jbgb.driver.entity

import com.fasterxml.jackson.annotation.JsonValue

enum class CarType(val value: String) {

    PKW("P"),
    LIMOUSINE("L"),
    OTHER("O");

    @JsonValue
    override fun toString() = value

    companion object {
        private val nameCache = HashMap<String, CarType>().apply {
            enumValues<CarType>().forEach {
                put(it.value, it)
                put(it.value.toLowerCase(), it)
                put(it.name, it)
                put(it.name.toLowerCase(), it)
            }
        }

        fun build(value: String?) = nameCache[value]
    }
}
