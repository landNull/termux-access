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

// Installs the WebDAV routes and the PROPFIND interceptor on the Ktor application.
// Every path is passed through resolveSafe before any filesystem access.
fun Application.installWebDav(appsRoot: File) {
    routing {
        route("/{path...}") {
            // Advertises the methods this server currently claims to support.
            options {
                call.response.headers.append("DAV", "1")
                call.response.headers.append("Allow", "OPTIONS, GET, HEAD, PUT, DELETE, MKCOL, PROPFIND, COPY, MOVE")
                call.respond(HttpStatusCode.OK)
            }
            // Streams a file. Honors a single bytes= range and returns 206 when present.
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
            // Creates or overwrites a file by streaming the request body.
            put {
                val file = resolveSafe(appsRoot, call.parameters.getAll("path")?.joinToString("/") ?: "")
                file.parentFile?.mkdirs()
                file.outputStream().use { out ->
                    call.receiveChannel().toInputStream().copyTo(out)
                }
                call.respond(HttpStatusCode.Created)
            }
            // Deletes the file or directory tree if it exists.
            delete {
                val file = resolveSafe(appsRoot, call.parameters.getAll("path")?.joinToString("/") ?: "")
                if (file.deleteRecursively()) call.respond(HttpStatusCode.NoContent) else call.respond(HttpStatusCode.NotFound)
            }
        }
    }
    // PROPFIND is handled outside the normal route table so Depth and Multi-Status can be returned.
    intercept(io.ktor.server.application.ApplicationCallPipeline.Plugins) {
        if (call.request.httpMethod.value.equals("PROPFIND", ignoreCase = true)) {
            val relative = call.parameters.getAll("path")?.joinToString("/") ?: ""
            val file = try { resolveSafe(appsRoot, relative) } catch (e: Exception) {
                call.respond(HttpStatusCode.Forbidden)
                finish()
                return@intercept
            }
            if (!file.exists()) {
                call.respond(HttpStatusCode.NotFound)
                finish()
                return@intercept
            }
            val depth = call.request.headers["Depth"] ?: "1"
            val xml = buildPropfind(file, relative, depth)
            call.response.headers.append("Content-Type", "application/xml; charset=utf-8")
            call.respondText(xml, ContentType.Application.Xml, HttpStatusCode.MultiStatus)
            finish()
        }
    }
}

// Builds a minimal 207 Multi-Status XML body for Depth 0 or 1.
private fun buildPropfind(file: File, relative: String, depth: String): String {
    val sb = StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<D:multistatus xmlns:D=\"DAV:\">\n")
    fun add(f: File, path: String) {
        val href = if (path.isEmpty()) "/" else "/$path"
        val dir = f.isDirectory
        val lm = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC).format(Instant.ofEpochMilli(f.lastModified()))
        sb.append("<D:response><D:href>$href</D:href><D:propstat><D:prop>")
        sb.append("<D:displayname>${f.name.ifEmpty { "Apps" }}</D:displayname>")
        sb.append("<D:getlastmodified>$lm</D:getlastmodified>")
        sb.append("<D:resourcetype>${if (dir) "<D:collection/>" else ""}</D:resourcetype>")
        if (!dir) sb.append("<D:getcontentlength>${f.length()}</D:getcontentlength>")
        sb.append("</D:prop><D:status>HTTP/1.1 200 OK</D:status></D:propstat></D:response>\n")
    }
    add(file, relative)
    if (depth != "0" && file.isDirectory) {
        file.listFiles()?.forEach { child ->
            val childPath = if (relative.isEmpty()) child.name else "$relative/${child.name}"
            add(child, childPath)
        }
    }
    sb.append("</D:multistatus>")
    return sb.toString()
}

// Canonicalizes the path and rejects anything that escapes the Apps root.
private fun resolveSafe(root: File, relative: String): File {
    val candidate = File(root, relative)
    val canonical = candidate.canonicalFile
    val rootCanonical = root.canonicalFile
    if (!canonical.path.startsWith(rootCanonical.path)) {
        throw SecurityException("Path traversal blocked")
    }
    return canonical
}
