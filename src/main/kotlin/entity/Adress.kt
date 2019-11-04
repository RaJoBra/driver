package de.jbgb.driver.entity

import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Pattern

data class Adress(
    @get:NotEmpty(message = "{adress.zip.notEmpty}")
    @get:Pattern(regexp = "\\d{5}", message = "{adress.zip}")
    val zip: String,

    @get:NotEmpty(message = "{adress.place.notEmpty}")
    val place: String

)
