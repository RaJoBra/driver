package de.jbgb.driver

import de.jbgb.driver.entity.Driver
// import de.jbgb.driver.html.HtmlHandler
import de.jbgb.driver.rest.DriverHandler
import de.jbgb.driver.rest.DriverStreamHandler
import org.springframework.context.annotation.Bean
import org.springframework.hateoas.MediaTypes.HAL_JSON
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.TEXT_EVENT_STREAM
// import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.web.reactive.function.server.router

interface Router {
    @Bean
    @Suppress("SpringJavaInjectionPointsAutowiringInspection", "LongMethod")
    fun router(
        handler: DriverHandler,
        streamHandler: DriverStreamHandler
        // htmlHandler: HtmlHandler
    ) = router {

        "/".nest {
            accept(HAL_JSON).nest {
                GET("/", handler::find)
                GET("/$idPathPattern", handler::findById)
            }

            contentType(APPLICATION_JSON).nest {
                POST("/", handler::create)
                PUT("/$idPathPattern", handler::update)
                PATCH("/$idPathPattern", handler::patch)
            }

            DELETE("/$idPathPattern", handler::deleteById)
            DELETE("/", handler::deleteByEmail)

            accept(TEXT_EVENT_STREAM).nest {
                GET("/", streamHandler::findAll)
            }

//            accept(TEXT_HTML).nest {
//                GET("/home", htmlHandler::home)
//                GET("/suche", htmlHandler::find)
//                GET("/details", htmlHandler::details)
//            }
        }
    }

    companion object {
    const val idPathVar = "id"

    private const val idPathPattern = "{$idPathVar:${Driver.ID_PATTERN}}"
    }
}
