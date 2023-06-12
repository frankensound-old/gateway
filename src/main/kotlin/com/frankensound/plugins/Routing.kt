package com.frankensound.plugins

import com.frankensound.ResponseS3
import com.frankensound.getObject
import com.frankensound.listBucketObs
import io.github.oshai.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.partialcontent.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

const val bucketName = "frankensound"

fun Application.configureRouting() {
    val logger = KotlinLogging.logger {}

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            cause.printStackTrace()
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }

    install(PartialContent) {
        maxRangeCount = 1
    }

    routing {
        val client = HttpClient(CIO)

        // Return a list of songs
        get("/songs") {
            logger.info { "Returned songs" }
            call.respondText(listBucketObs(bucketName).toString())
        }

        // Play a song, streaming chunks
        get("/songs/play/{title}") {
            val key: String = call.parameters["title"].toString()

            val header = call.request.header(HttpHeaders.Range).toString()
            if (!header.contains("=")) {
                return@get call.respond(HttpStatusCode.BadRequest)
            }
            val unit = header.substringBefore("=").ifEmpty { null } ?: return@get call.respond(HttpStatusCode.BadRequest)
            if (unit != "bytes") {
                return@get call.respond(HttpStatusCode.BadRequest)
            }
            val range = header.substringAfter("=").split("-").ifEmpty { null } ?: return@get call.respond(HttpStatusCode.BadRequest)
            val start = range.getOrNull(0)?.ifBlank { null }?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
            val end = range.getOrNull(1)?.ifBlank { null }?.toIntOrNull() ?: (start + 500000)

            if(end < start){
                return@get call.respond(HttpStatusCode.BadRequest)
            }
            val rangeVal = "$unit=$start-$end"

            val responseS3: ResponseS3 = getObject(bucketName, key, rangeVal) ?: return@get call.respond(HttpStatusCode.NotFound)

            call.response.headers.append(HttpHeaders.AcceptRanges, "bytes")

            call.response.headers.append(HttpHeaders.ContentRange, responseS3.contentRange!!)

            return@get call.respondBytes(responseS3.data, status = HttpStatusCode.PartialContent)
        }

        //Return play history of user
        get("/profile/history") {
            val response: HttpResponse = client.get("http://host.docker.internal:8090")

            if (response.status.value in 200..299) {
                call.response.status(HttpStatusCode.OK)
            }
            call.respondText(response.body())
        }
        //Return recommendation made for user
        get("profile/recommendations") {
            //TODO: Implement recommendations endpoint
        }
    }
}
