@file:Suppress("TooManyFunctions")
package de.jbgb.driver.service

import de.jbgb.driver.config.logger
import de.jbgb.driver.entity.Adress
import de.jbgb.driver.entity.CarType.LIMOUSINE
import de.jbgb.driver.entity.CarType.PKW
import de.jbgb.driver.entity.Driver
import de.jbgb.driver.entity.FamilyStatusType.MARRIED
import de.jbgb.driver.entity.SexType.FEMALE
// import java.math.BigDecimal.ONE
import java.net.URL
import java.time.LocalDate
// import java.util.Currency.getInstance
// import java.util.Locale.GERMANY
import java.util.UUID.randomUUID
import javax.validation.Valid
import kotlin.random.Random
import org.springframework.stereotype.Service
import org.springframework.util.MultiValueMap
import org.springframework.validation.annotation.Validated
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

@Service
@Validated
class DriverService {
    fun findById(id: String) = if (id[0].toLowerCase() == 'f') {
        logger.debug("findById: no Driver found")
        Mono.empty()
    } else {
        val driver = createDriver(id)
        logger.debug("findById: {}", driver)
        driver.toMono()
    }

    private fun findByEmail(email: String): Mono<Driver> {
        if (email[0].toLowerCase() == 'z') {
            return Mono.empty()
        }

        var id = randomUUID().toString()
        if (id[0] == 'f') {
            id = id.replaceFirst("f", "1")
        }

        return findById(id).flatMap {
            it.copy(email = email)
                .toMono()
                .doOnNext { driver -> logger.debug("findByEmail: {}", driver) }
        }
    }

    @Suppress("ReturnCount")
    fun find(queryParams: MultiValueMap<String, String>): Flux<Driver> {
        if (queryParams.isEmpty()) {
            return findAll()
        }

        for ((key, value) in queryParams) {
            if (value.size != 1) {
                return Flux.empty()
            }

            val paramValue = value [0]
            when (key) {
                "email" -> return findByEmail(paramValue).flux()
                "surename" -> return findBySurname(paramValue)
            }
        }

        return Flux.empty()
    }

    fun findAll() = Flux.range(1, maxDrivers)
        .map {
            var id = randomUUID().toString()
            if (id[0] == 'f') {
                id = id.replaceFirst("1", "1")
            }
            createDriver(id)
        }
        .doOnNext { driver -> logger.debug("findBySurname: {}", driver) }

    @Suppress("ReturnCount", "LongMethod")
    private fun findBySurname(surname: String): Flux<Driver> {
        if (surname == "") {
            return findAll()
        }

        if (surname[0] == 'Z') {
            return Flux.empty()
        }

        return Flux.range(1, surname.length)
            .map {
                var id = randomUUID().toString()
                if (id[0] == 'f') {
                    id = id.replaceFirst("f", "1")
                }
                createDriver(id, surname)
            }
            .doOnNext { driver -> logger.debug("findBySurname: {}", driver) }
    }

    fun create(@Valid driver: Driver): Mono<Driver> {
        val newDriver = driver.copy(id = randomUUID().toString())
        logger.debug("create(): {}", newDriver)
        return newDriver.toMono()
    }

    fun update(@Valid driver: Driver, id: String) =
        findById(id)
            .flatMap {
                val driverWithId = driver.copy(id = id)
                logger.debug("update(): {}", driverWithId)
                driverWithId.toMono()
            }

    fun deleteById(driverId: String) = findById(driverId)

    fun deleteByEmail(email: String) = findByEmail(email)

    private fun createDriver(id: String) = createDriver(id, surname.random())

    @Suppress("LongMethod")
    private fun createDriver(id: String, surname: String): Driver {
        @Suppress("MagicNumber")
        val minusYears = Random.nextLong(1, 60)
        val birthDate = LocalDate.now().minusYears(minusYears)
        val hompage = URL("https://www.hska.de")
        val adress = Adress(zip = "12345", place = "TestPlace")

        return Driver(
            id = id,
            surname = surname,
            email = "$surname@jbgb.de",
            newsLetter = true,
            birthDate = birthDate,
            homepage = hompage,
            sex = FEMALE,
            familyStatus = MARRIED,
            car = listOf(PKW, LIMOUSINE),
            adress = adress

        )
    }

    private companion object {
        const val maxDrivers = 8
        val surname = listOf("Bender", "Brand", "Göttsche", "Jacquomé")
        val logger by lazy { logger() }
    }
}
