package de.jbgb.driver.rest.hateoas

import de.jbgb.driver.entity.Driver
import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.Link
import org.springframework.hateoas.server.SimpleRepresentationModelAssembler
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest

@Component
class ListDriverModelAssembler : SimpleRepresentationModelAssembler<Driver> {
    fun toModel(driver: Driver, request: ServerRequest): EntityModel<Driver> {
        val uri = request.uri().toString()
        val baseUri = uri.substringBefore('?').removeSuffix("/")
        val idUri = "$baseUri/${driver.id}"

        val selfLink = Link(idUri)
        return toModel(driver).add(selfLink)
    }

    override fun addLinks(model: EntityModel<Driver>) = Unit

    override fun addLinks(model: CollectionModel<EntityModel<Driver>>) = Unit
}
