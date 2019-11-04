package de.jbgb.driver.entity

import com.fasterxml.jackson.annotation.JsonValue

enum class FamilyStatusType(val value: String) {
    SINGLE("S"),
    MARRIED("M"),
    DIVORCED("D"),
    WIDOWED("W");

    @JsonValue
    override fun toString() = value

    companion object {
        private val nameCache = HashMap<String, FamilyStatusType>().apply {
            enumValues<FamilyStatusType>().forEach {
                put(it.value, it)
                put(it.value.toLowerCase(), it)
                put(it.name, it)
                put(it.name.toLowerCase(), it)
            }
        }

        fun build(value: String?) = nameCache[value]
    }
}
