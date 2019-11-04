@file:Suppress("PackageDirectoryMismatch")

package de.jbgb.driver.rest

import com.jayway.jsonpath.JsonPath
import de.jbgb.driver.config.Settings.DEV
import de.jbgb.driver.entity.Adress
import de.jbgb.driver.entity.CarType.LIMOUSINE
import de.jbgb.driver.entity.CarType.PKW
import de.jbgb.driver.entity.Driver
import de.jbgb.driver.entity.SexType.FEMALE
import de.jbgb.driver.rest.constraints.DriverConstraintViolation
import de.jbgb.driver.rest.patch.PatchOperation
// import java.math.BigDecimal
import java.net.URI
import java.net.URL
import java.time.LocalDate
// import java.util.Currency
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnJre
import org.junit.jupiter.api.condition.JRE.JAVA_10
import org.junit.jupiter.api.condition.JRE.JAVA_11
import org.junit.jupiter.api.condition.JRE.JAVA_8
import org.junit.jupiter.api.condition.JRE.JAVA_9
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.aggregator.ArgumentsAccessor
import org.junit.jupiter.params.aggregator.get
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.getBean
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.web.reactive.context.ReactiveWebApplicationContext
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.hateoas.mediatype.hal.HalLinkDiscoverer
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType.TEXT_EVENT_STREAM
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.body
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.kotlin.core.publisher.toMono

@Tag("rest")
@DisplayName("test REST-Interface for Driver")
@ExtendWith(SpringExtension::class, SoftAssertionsExtension::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles(DEV)
@TestPropertySource(locations = ["/rest-test.properties"])
@DisabledOnJre(value = [JAVA_8, JAVA_9, JAVA_10, JAVA_11])
@Suppress("ClassName")
class DriverRestTest(@LocalServerPort private val port: Int, ctx: ReactiveWebApplicationContext) {
    private var baseUrl = "http://$HOST:$port"
    private var client = WebClient.builder()
        .filter(basicAuthentication(USERNAME, PASSWORD))
        .baseUrl(baseUrl)
        .build()

    init {
        assertThat(ctx.getBean<DriverHandler>()).isNotNull
        assertThat(ctx.getBean<DriverStreamHandler>()).isNotNull
    }

    @Test
    fun `Always Succesfull`() {
        @Suppress("UsePropertyAccessSyntax")
        assertThat(true).isTrue()
    }

    @Test
    @Disabled("Noch nicht fertig")
    fun `Not findished yet`() {
        @Suppress("UsePropertyAccessSyntax")
        assertThat(false).isFalse()
    }

    // -------------------------------------------------------------------------
    // L E S E N
    // -------------------------------------------------------------------------
    @Nested
    inner class Read {
        @Nested
        inner class `Search with ID` {
            @ParameterizedTest
            @ValueSource(strings = [ID_VORHANDEN])
            @WithMockUser(USERNAME, roles = [ADMIN])
            @Order(1000)
            fun `Search with ID`(id: String, softly: SoftAssertions) {
                // act
                val response = client.get()
                    .uri(ID_PATH, id)
                    .exchange()
                    .block()

                // assert
                assertThat(response).isNotNull
                response as ClientResponse
                assertThat(response.statusCode()).isEqualTo(OK)
                val content = response.bodyToMono(String::class.java).block()
                assertThat(content).isNotNull()
                content as String

                with(softly) {
                    val surname: String = JsonPath.read(content, "$.surname")
                    assertThat(surname).isNotBlank
                    val email: String = JsonPath.read(content, "$.email")
                    assertThat(email).isNotBlank
                    val linkDiscoverer = HalLinkDiscoverer()
                    val selfLink = linkDiscoverer.findLinkWithRel("self", content).get().href
                    assertThat(selfLink).endsWith("/$id")
                }
            }

            @ParameterizedTest
            @ValueSource(strings = [ID_INVALID, ID_NICHT_VORHANDEN])
            @WithMockUser(USERNAME, roles = [ADMIN])
            @Order(1100)
            fun `Search with invalid or wrong ID`(id: String) {
                // act
                val response = client.get()
                    .uri(ID_PATH, id)
                    .exchange()
                    .block()

                // assert
                assertThat(response?.statusCode()).isEqualTo(NOT_FOUND)
            }
        }

        @Test
        @WithMockUser(USERNAME, roles = [ADMIN])
        @Order(2000)
        fun `Suche nach allen Drivers `() {
            // act
            val drivers = client.get()
                .retrieve()
                .bodyToFlux<Driver>()
                .collectList()
                .block()

            // assert
            assertThat(drivers)
                .isNotNull
                .isNotEmpty
        }

        @ParameterizedTest
        @ValueSource(strings = [SURNAME])
        @WithMockUser(USERNAME, roles = [ADMIN])
        @Order(2100)
        fun `Suche mit vorhandenem Nachnamen`(surname: String) {
            // arrange
            val surnameLower = surname.toLowerCase()

            // act
            val drivers = client.get()
                .uri {
                    it.path(DRIVER_PATH)
                        .queryParam(SURNAME_PARAM, surnameLower)
                        .build()
                }
                .retrieve()
                .bodyToFlux<Driver>()
                .collectList()
                .block()

            // assert
            assertThat(drivers)
                .isNotEmpty
                .allMatch { driver -> driver.surname.toLowerCase() == surnameLower }
        }

        @Test
        @WithMockUser(USERNAME, roles = [ADMIN])
        @Order(2200)
        fun `Search all driver with Streaming`() {
            // act
            val drivers = client.get()
                .uri("/")
                .accept(TEXT_EVENT_STREAM)
                .exchange()
                .flatMapMany { it.bodyToFlux<Driver>() }
                .collectList()
                .block()

            // assert
            assertThat(drivers).isNotEmpty
        }
    }

    // -------------------------------------------------------------------------
    // S C H R E I B E N
    // -------------------------------------------------------------------------
    @Nested
    inner class Write {
        @Nested
        inner class Create {
            @ParameterizedTest
            @CsvSource(
                "$NEW_SURNAME, $NEW_EMAIL, $NEW_BIRTHDATE, $NEW_HOMEPAGE, $NEW_ZIP, " +
                    NEW_PLACE
            )
            @Order(5000)
            fun `Neuanlegen eines neuen Drivers `(args: ArgumentsAccessor) {
                // arrange
                val neuerDriver = Driver(
                    id = null,
                    surname = args.get<String>(0),
                    email = args.get<String>(1),
                    newsLetter = true,
                    birthDate = args.get<LocalDate>(2),
                    homepage = args.get<URL>(4),
                    sex = FEMALE,
                    familyStatus = null,
                    car = listOf(PKW, LIMOUSINE),
                    adress = Adress(zip = args.get<String>(5), place = args.get<String>(6))
                )

                // act
                val response = client.post()
                    .body(neuerDriver.toMono())
                    .exchange()
                    .block()

                // assert
                assertThat(response).isNotNull
                response as ClientResponse
                assertThat(response.statusCode()).isEqualTo(CREATED)
                assertThat(response.headers()).isNotNull
                val location = response.headers().asHttpHeaders().location
                assertThat(location).isNotNull()
                location as URI
                val locationStr = location.toString()
                assertThat(locationStr).isNotBlank()
                val indexLastSlash = locationStr.lastIndexOf('/')
                assertThat(indexLastSlash).isPositive()
                val id = locationStr.substring(indexLastSlash + 1)
                assertThat(id).isNotNull()
            }

            @ParameterizedTest
            @CsvSource(
                "$NEW_SURNAME_INVALID, $NEW_EMAIL_INVALID, " +
                    "$NEW_ZIP_INVALID, $NEW_PLACE, $NEW_BIRTHDATE"
            )
            @Order(5100)
            fun `Neuanlegen eines neuen Drivers  mit ungueltigen Werten`(
                surname: String,
                email: String,
                zip: String,
                place: String,
                birthDate: LocalDate
            ) {
                // arrange
                val adress = Adress(zip = zip, place = place)
                val neuerDriver = Driver(
                    id = null,
                    surname = surname,
                    email = email,
                    newsLetter = true,
                    birthDate = birthDate,
                    homepage = null,
                    sex = FEMALE,
                    familyStatus = null,
                    car = listOf(PKW, LIMOUSINE),
                    adress = adress
                )

                // act
                val response = client.post()
                    .body(neuerDriver.toMono())
                    .exchange()
                    .block()

                // assert
                assertThat(response).isNotNull
                response as ClientResponse
                with(response) {
                    assertThat(statusCode()).isEqualTo(BAD_REQUEST)
                    val violations =
                        bodyToFlux<DriverConstraintViolation>().collectList().block()
                    assertThat(violations).isNotNull
                    violations as List<DriverConstraintViolation>
                    assertThat(violations)
                        .hasSize(3)
                        .doesNotHaveDuplicates()
                    val violationMsgPredicate = { msg: String ->
                        msg.contains("doesn't have 5 digits") ||
                            msg.contains("Surname has a") ||
                            msg.contains("The EMail-Adress")
                    }
                    violations
                        .map { it.message!! }
                        .forEach { msg ->
                            assertThat(msg).matches(violationMsgPredicate)
                        }
                }
            }
        }

        @Nested
        inner class Change {
            @ParameterizedTest
            @ValueSource(strings = [ID_UPDATE_PUT])
            @WithMockUser(USERNAME, roles = [ADMIN])
            @Order(6000)
            fun `Change avaible Driver with PUT`(id: String) {
                // arrange
                val driverOrig = client.get()
                    .uri(ID_PATH, id)
                    .retrieve()
                    .bodyToMono<Driver>()
                    .block()
                assertThat(driverOrig).isNotNull
                driverOrig as Driver
                val driver = Driver(
                    id = null,
                    surname = driverOrig.surname,
                    email = "${driverOrig.email}put",
                    adress = driverOrig.adress,
                    familyStatus = null,
                    birthDate = null,
                    homepage = null,
                    sex = FEMALE,
                    car = null,
                    newsLetter = false
                    )

                // act
                val response = client.put()
                    .uri(ID_PATH, id)
                    .body(driver.toMono())
                    .exchange()
                    .block()

                // assert
                assertThat(response).isNotNull
                response as ClientResponse
                assertThat(response.statusCode()).isEqualTo(NO_CONTENT)
                val hasBody = response.bodyToMono<String>()
                    .hasElement()
                    .block()
                assertThat(hasBody).isFalse()
            }

            @ParameterizedTest
            @CsvSource("$ID_UPDATE_PATCH, $NEW_EMAIL")
            @WithMockUser(USERNAME, roles = [ADMIN])
            @Order(6100)
            fun `Change a avaible Driver with PATCH`(id: String, email: String) {
                // arrange
                val replaceOp = PatchOperation(
                    op = "replace",
                    path = "/email",
                    value = email
                )
                val addOp = PatchOperation(
                    op = "add",
                    path = "/car",
                    value = NEW_CAR.value
                )
                val removeOp = PatchOperation(
                    op = "remove",
                    path = "/car",
                    value = TO_DELETE_CAR.value
                )
                val operations = listOf(replaceOp, addOp, removeOp)

                // act
                val response = client.patch()
                    .uri(ID_PATH, id)
                    .body(operations.toMono())
                    .exchange()
                    .block()

                // assert
                assertThat(response).isNotNull
                response as ClientResponse
                assertThat(response.statusCode()).isEqualTo(NO_CONTENT)
                val hasBody = response.bodyToMono<String>()
                    .hasElement()
                    .block()
                assertThat(hasBody).isFalse()
            }
        }

        @Nested
        inner class Delete {
            @ParameterizedTest
            @ValueSource(strings = [ID_DELETE])
            @WithMockUser(USERNAME, roles = [ADMIN])
            @Order(7000)
            fun `Delte avaibale Driver `(id: String) {
                // act
                val response = client.delete()
                    .uri(ID_PATH, id)
                    .exchange()
                    .block()

                // assert
                assertThat(response?.statusCode()).isEqualTo(NO_CONTENT)
            }

            @ParameterizedTest
            @ValueSource(strings = [EMAIL_DELETE])
            @WithMockUser(USERNAME, roles = [ADMIN])
            @Order(7100)
            fun `Delte driver with E-MAil`(email: String) {
                // act
                val response = client.delete()
                    .uri {
                        it.path(DRIVER_PATH)
                            .queryParam(EMAIL_PARAM, email)
                            .build()
                    }
                    .exchange()
                    .block()

                // assert
                @Suppress("UsePropertyAccessSyntax")
                assertThat(response?.statusCode()).isEqualTo(NO_CONTENT)
            }
        }
    }

    private companion object {
        const val HOST = "localhost"
        const val DRIVER_PATH = "/"
        const val USERNAME = "admin"
        const val PASSWORD = "p"
        const val ADMIN = "ADMIN"

        const val ID_VORHANDEN = "10000000-0000-0000-0000-000000000001"
        const val ID_INVALID = "10000000-0000-0000-0000-00000000000X"
        const val ID_NICHT_VORHANDEN = "f0000000-0000-0000-0000-000000000001"
        const val ID_UPDATE_PUT = "10000000-0000-0000-0000-000000000002"
        const val ID_UPDATE_PATCH = "10000000-0000-0000-0000-000000000003"
        const val ID_DELETE = "10000000-0000-0000-0000-000000000005"

        const val SURNAME = "alpha"

        const val NEW_ZIP = "12345"
        const val NEW_PLACE = "Testplace"
        const val NEW_SURNAME = "NewSurname"
        const val NEW_EMAIL = "email@test.de"
        const val NEW_BIRTHDATE = "2017-01-31"
        const val NEW_HOMEPAGE = "https://test.de"

        const val NEW_ZIP_INVALID = "1234"
        const val NEW_SURNAME_INVALID = "?!$"
        const val NEW_EMAIL_INVALID = "email@"

        val NEW_CAR = PKW
        val TO_DELETE_CAR = LIMOUSINE

        const val EMAIL_DELETE = "foo@bar.test"

        const val ID_PATH = "/{id}"
        const val SURNAME_PARAM = "surname"
        const val EMAIL_PARAM = "email"
    }
}
