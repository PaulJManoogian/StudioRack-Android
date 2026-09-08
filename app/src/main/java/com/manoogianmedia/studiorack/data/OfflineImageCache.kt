package com.manoogianmedia.studiorack.data

import android.content.Context
import com.manoogianmedia.studiorack.R
import java.io.File
import java.net.URL
import java.security.MessageDigest

internal fun resolvedAssetUrl(value: String, publicBaseUrl: String): String? {
    val source = value.trim()
    if (source.isBlank()) return null
    val base = publicBaseUrl.trimEnd('/')
    val origin = URL(base).let { "${it.protocol}://${it.authority}" }
    return when {
        source.startsWith("https://", true) || source.startsWith("http://", true) -> source
        source.startsWith("/") -> "$origin$source"
        else -> "$base/${source.trimStart('/')}"
    }
}

internal fun cacheImageFile(context: Context, imageUrl: String): File? {
    val resolved = resolvedAssetUrl(imageUrl, context.getString(R.string.public_base_url)) ?: return null
    val directory = File(context.filesDir, "offline-images").also(File::mkdirs)
    val digest = MessageDigest.getInstance("SHA-256").digest(resolved.toByteArray()).joinToString("") { "%02x".format(it) }
    val cached = File(directory, "$digest.image")
    if (!cached.isFile || cached.length() == 0L) {
        runCatching {
            val connection = URL(resolved).openConnection().apply {
                connectTimeout = 8_000
                readTimeout = 12_000
            }
            connection.getInputStream().use { input -> cached.outputStream().use { output -> input.copyTo(output) } }
        }.onFailure { cached.delete() }
    }
    return cached.takeIf { it.isFile && it.length() > 0L }
}
