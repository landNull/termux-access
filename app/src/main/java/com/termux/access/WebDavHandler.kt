package com.termux.access

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import io.ktor.server.routing.route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.delete
import io.ktor.server.routing.options
import io.ktor.utils.io.jvm.javaio.toInputStream
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun Application.installWebDav(appsRoot: File) {
    routing {
        route("/{path...}") {
            options {
                call.response.headers.append("DAV", "1")
                call.response.headers.append("Allow", "OPTIONS, GET, HEAD, PUT, DELETE, MKCOL, PROPFIND, COPY, MOVE")
                call.respond(HttpStatusCode.OK)
            }
            get {
                val file = resolveSafe(appsRoot, call.parameters.getAll("path")?.joinToString("/") ?: "")
                if (!file.exists()) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }
                if (file.isDirectory) {
                    call.respondText("Use PROPFIND for directories", ContentType.Text.Plain)
                    return@get
                }
                val rangeHeader = call.request.headers["Range"]
                if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                    val range = rangeHeader.removePrefix("bytes=").split("-")
                    val start = range[0].toLongOrNull() ?: 0L
                    val end = range.getOrNull(1)?.toLongOrNull() ?: (file.length() - 1)
                    call.respondOutputStream(ContentType.Application.OctetStream, HttpStatusCode.PartialContent) {
                        FileChannel.open(file.toPath(), StandardOpenOption.READ).use { channel ->
                            channel.position(start)
                            val buffer = ByteArray(8192)
                            var remaining = end - start + 1
                            while (remaining > 0) {
                                val read = channel.read(java.nio.ByteBuffer.wrap(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt()))
                                if (read <= 0) break
                                write(buffer, 0, read)
                                remaining -= read
                            }
                        }
                    }
                    call.response.headers.append("Content-Range", "bytes $start-$end/${file.length()}")
                    call.response.headers.append("Accept-Ranges", "bytes")
                } else {
                    call.respondOutputStream(ContentType.Application.OctetStream) {
                        FileChannel.open(file.toPath(), StandardOpenOption.READ).use { channel ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            while (channel.read(java.nio.ByteBuffer.wrap(buffer)).also { read = it } > 0) {
                                write(buffer, 0, read)
                            }
                        }
                    }
                    call.response.headers.append("Accept-Ranges", "bytes")
                }
            }
            put {
                val file = resolveSafe(appsRoot, call.parameters.getAll("path")?.joinToString("/") ?: "")
                file.parentFile?.mkdirs()
                file.outputStream().use { out ->
                    call.receiveChannel().toInputStream().copyTo(out)
                }
                call.respond(HttpStatusCode.Created)
            }
            delete {
                val file = resolveSafe(appsRoot, call.parameters.getAll("path")?.joinToString("/") ?: "")
                if (file.deleteRecursively()) call.respond(HttpStatusCode.NoContent) else call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}

private fun resolveSafe(root: File, relative: String): File {
    val candidate = File(root, relative)
    val canonical = candidate.canonicalFile
    val rootCanonical = root.canonicalFile
    if (!canonical.path.startsWith(rootCanonical.path)) {
        throw SecurityException("Path traversal blocked")
    }
    return canonical
}
