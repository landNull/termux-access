package com.termux.access

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import java.io.File
import java.io.FileNotFoundException

// DocumentsProvider that exposes only the Termux $HOME/Apps subtree via Storage Access Framework.
// All paths are resolved relative to appsRoot and rejected if they escape it.
class AccessDocumentsProvider : DocumentsProvider() {
    private val rootId = "root"
    private val appsRoot = File("/data/data/com.termux/files/home/Apps")

    override fun onCreate(): Boolean = true

    // Returns the single root that clients see as "Termux Apps".
    override fun queryRoots(projection: Array<out String>?): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION)
        val row = result.newRow()
        row.add(DocumentsContract.Root.COLUMN_ROOT_ID, rootId)
        row.add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, rootId)
        row.add(DocumentsContract.Root.COLUMN_TITLE, "Termux Apps")
        row.add(DocumentsContract.Root.COLUMN_FLAGS, DocumentsContract.Root.FLAG_SUPPORTS_CREATE)
        row.add(DocumentsContract.Root.COLUMN_ICON, R.mipmap.ic_launcher)
        return result
    }

    // Returns metadata for one document after confirming it stays inside appsRoot.
    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor {
        val file = resolveSafe(documentId)
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        includeFile(result, documentId, file)
        return result
    }

    // Lists children of a directory document, again after the safe-path check.
    override fun queryChildDocuments(parentDocumentId: String, projection: Array<out String>?, sortOrder: String?): Cursor {
        val parent = resolveSafe(parentDocumentId)
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        if (parent.isDirectory) {
            parent.listFiles()?.forEach { child ->
                val childId = if (parentDocumentId == rootId) "$rootId/${child.name}" else "$parentDocumentId/${child.name}"
                includeFile(result, childId, child)
            }
        }
        return result
    }

    // Opens a file for read or read-write; throws if the path is unsafe or missing.
    override fun openDocument(documentId: String, mode: String, signal: CancellationSignal?): ParcelFileDescriptor {
        val file = resolveSafe(documentId)
        if (!file.exists()) throw FileNotFoundException("Not found: $documentId")
        val accessMode = if (mode.contains("w")) ParcelFileDescriptor.MODE_READ_WRITE else ParcelFileDescriptor.MODE_READ_ONLY
        return ParcelFileDescriptor.open(file, accessMode)
    }

    // Canonicalizes the candidate and rejects anything that does not start with the Apps root.
    // This is the sole guard against path traversal for the SAF surface.
    private fun resolveSafe(documentId: String): File {
        if (documentId == rootId) return appsRoot
        val relative = documentId.removePrefix("$rootId/")
        val candidate = File(appsRoot, relative)
        val canonical = candidate.canonicalFile
        val rootCanonical = appsRoot.canonicalFile
        if (!canonical.path.startsWith(rootCanonical.path)) {
            throw SecurityException("Path traversal blocked: $documentId")
        }
        return canonical
    }

    // Populates one row of a document cursor with the fields clients expect.
    private fun includeFile(result: MatrixCursor, documentId: String, file: File) {
        val row = result.newRow()
        row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, documentId)
        row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, file.name.ifEmpty { "Apps" })
        row.add(DocumentsContract.Document.COLUMN_SIZE, file.length())
        row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, if (file.isDirectory) DocumentsContract.Document.MIME_TYPE_DIR else "application/octet-stream")
        row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, file.lastModified())
        var flags = 0
        if (file.isDirectory) flags = flags or DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE
        if (file.canWrite()) {
            flags = flags or DocumentsContract.Document.FLAG_SUPPORTS_WRITE or DocumentsContract.Document.FLAG_SUPPORTS_DELETE
        }
        row.add(DocumentsContract.Document.COLUMN_FLAGS, flags)
    }

    companion object {
        private val DEFAULT_ROOT_PROJECTION = arrayOf(
            DocumentsContract.Root.COLUMN_ROOT_ID,
            DocumentsContract.Root.COLUMN_DOCUMENT_ID,
            DocumentsContract.Root.COLUMN_TITLE,
            DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.COLUMN_ICON
        )
        private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS
        )
    }
}
