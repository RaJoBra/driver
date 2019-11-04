/*
 * Copyright (C) 2013 - present Juergen Zimmermann, Hochschule Karlsruhe
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.jbgb.driver.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import de.jbgb.driver.entity.Driver.Companion.ID_PATTERN
import de.jbgb.driver.entity.Driver.Companion.SURNAME_PATTERN
import java.net.URL
import java.time.LocalDate
import javax.validation.Valid
import javax.validation.constraints.Email
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Past
import javax.validation.constraints.Pattern
import org.hibernate.validator.constraints.UniqueElements

@JsonPropertyOrder(
    "surname", "email", "newLetter", "birthDate", "homepage", "sex", "FamilySatus", "carType", "adress"
)

data class Driver(
    @get:Pattern(regexp = ID_PATTERN, message = "{driver.id.pattern}")
    @JsonIgnore
    val id: String?,

    @get:NotEmpty(message = "{driver.surename.notEmpty}")
    @get:Pattern(
        regexp = SURNAME_PATTERN,
        message = "{driver.surename.pattern}"
    )
    val surname: String,

    @get:Email(message = "{driver.email.pattern}")
    val email: String,

    val newsLetter: Boolean = false,

    @get:Past(message = "{driver.birthDate.past}")
    val birthDate: LocalDate?,

    val sex: SexType,

    val familyStatus: FamilyStatusType?,

    val homepage: URL?,

    @get:UniqueElements(message = "{driver.interests.uniqueElements}")
    val car: List<CarType>?,

    @get:Valid
    val adress: Adress

) {
    @Suppress("ReturnCount")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Driver
        return email == other.email
    }

    override fun hashCode() = email.hashCode()

    companion object {
        private const val HEX_PATTERN = "[\\dA-Fa-f]"

        const val ID_PATTERN =
            "$HEX_PATTERN{8}-$HEX_PATTERN{4}-$HEX_PATTERN{4}-" +
                "$HEX_PATTERN{4}-$HEX_PATTERN{12}"

        private const val SURNAME_PREFIX = "o'|von|von der|von und zu|van"

        private const val NAME_PATTERN = "[A-ZÄÖÜ][a-zäöüß]+"

        const val SURNAME_PATTERN =
            "($SURNAME_PREFIX)?$NAME_PATTERN(-$NAME_PATTERN)?"
    }
}
