package de.jbgb.driver.rest

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import de.jbgb.driver.Router.Companion.idPathVar
import de.jbgb.driver.config.logger
import de.jbgb.driver.entity.Driver
import de.jbgb.driver.rest.constraints.DriverConstraintViolation
import de.jbgb.driver.rest.hateoas.DriverModelAssembler
import de.jbgb.driver.rest.hateoas.ListDriverModelAssembler
// import de.jbgb.driver.rest.patch.InvalidCarException
import de.jbgb.driver.rest.patch.DriverPatcher
import de.jbgb.driver.rest.patch.PatchOperation
import de.jbgb.driver.service.DriverService
import java.net.URI
import javax.validation.ConstraintViolationException
import org.springframework.core.codec.DecodingException
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.badRequest
import org.springframework.web.reactive.function.server.ServerResponse.created
import org.springframework.web.reactive.function.server.ServerResponse.noContent
import org.springframework.web.reactive.function.server.ServerResponse.notFound
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.ServerResponse.status
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.bodyToFlux
import org.springframework.web.reactive.function.server.bodyToMono
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.onErrorResume
import reactor.kotlin.core.publisher.toMono

@Component
@Suppress("TooManyFunctions")
class DriverHandler(
    private val service: DriverService,
    private val modelAssembler: DriverModelAssembler,
    private val listModelAssember: ListDriverModelAssembler
) {
    fun findById(request: ServerRequest): Mono<ServerResponse> {
        val id = request.pathVariable(idPathVar)
        return service.findById(id)
            .doOnNext { driver -> logger.debug("findById: {}", driver) }
            .map { modelAssembler.toModel(it, request.exchange()) }
            .flatMap { ok().body(it.toMono()) }
            .switchIfEmpty(notFound().build())
    }

    fun find(request: ServerRequest): Mono<ServerResponse> {
        val queryParams = request.queryParams()

        return service.find(queryParams)
            .doOnNext { driver -> logger.debug("find: {}", driver) }
            .collectList()
            .flatMap { drivers ->
                if (queryParams.keys.contains("email")) {
                    val driversModel = modelAssembler.toModel(drivers[0], request.exchange())
                    ok().body(driversModel)
                } else {
                    val driversModel = drivers.map { driver -> listModelAssember.toModel(driver, request) }
                    ok().body(driversModel.toMono())
                }
            }
            .switchIfEmpty(notFound().build())
    }

    fun create(request: ServerRequest) =
        request.bodyToMono<Driver>()
            .flatMap { service.create(it) }
            .flatMap { driver ->
                logger.debug("create: {}", driver)
                val location = URI("${request.uri()}${driver.id}")
                created(location).build()
            }
            .onErrorResume(DecodingException::class) { handleDecodingException(it) }

    private fun handleConstraintViolation(exception: ConstraintViolationException, deleteStr: String):
        Mono<ServerResponse> {
            val violations = exception.constraintViolations
            if (violations.isEmpty()) {
                return badRequest().build()
            }

            val driverViolations = violations.map { violation ->
                DriverConstraintViolation(
                    property = violation.propertyPath.toString().replace(deleteStr, ""),
                    message = violation.message
                )
            }
            logger.debug("handleConstraintViolation(): {}", driverViolations)
            return badRequest().body(driverViolations.toMono())
        }

    private fun handleDecodingException(e: DecodingException) = when (val exception = e.cause) {
        is JsonParseException -> {
            logger.debug("handleDecodingException(): JsonParseException={}", exception.message)
            val msg = exception.message ?: ""
            badRequest().body(msg.toMono())
        }
        is InvalidFormatException -> {
            logger.debug("handleDecodingException(): InvalidFormatException={}", exception.message)
            val msg = exception.message ?: ""
            badRequest().body(msg.toMono())
        }
        else -> status(INTERNAL_SERVER_ERROR).build()
    }

    fun update(request: ServerRequest): Mono<ServerResponse> {
        val id = request.pathVariable(idPathVar)
        return request.bodyToMono<Driver>()
            .flatMap { service.update(it, id) }
            .flatMap { noContent().build() }
            .switchIfEmpty(notFound().build())
            .onErrorResume(ConstraintViolationException::class) {
                handleConstraintViolation(it, "update.driver")
            }
            .onErrorResume(DecodingException::class) { handleDecodingException(it) }
    }

    @Suppress("LongMehtod")
    fun patch(request: ServerRequest): Mono<ServerResponse> {
        val id = request.pathVariable(idPathVar)

        return request.bodyToFlux<PatchOperation>()
            .collectList()
            .flatMap { patchOps ->
                service.findById(id)
                    .flatMap {
                        val driverPatched = DriverPatcher.patch(it, patchOps)
                        logger.debug("patch(): {}", driverPatched)
                        service.update(driverPatched, id)
                    }
                    .flatMap { noContent().build() }
                    .switchIfEmpty(notFound().build())
            }
            .onErrorResume(ConstraintViolationException::class) {
                handleConstraintViolation(it, "update.driver.")
            }
            .onErrorResume(DecodingException::class) { handleDecodingException(it) }
    }

    fun deleteById(request: ServerRequest): Mono<ServerResponse> {
        val id = request.pathVariable(idPathVar)
        return service.deleteById(id).flatMap { noContent().build() }
    }

    fun deleteByEmail(request: ServerRequest): Mono<ServerResponse> {
        val email = request.queryParam("email")
        return if (email.isPresent) {
            return service.deleteByEmail(email.get())
                .flatMap { noContent().build() }
        } else {
            notFound().build()
        }
    }

    private companion object {
        val logger by lazy { logger() }
    }
}
