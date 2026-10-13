package com.mineinabyss.emojy.velocity

import com.mineinabyss.emojy.EmotePayload
import com.mineinabyss.emojy.EmoteRegistry
import com.mineinabyss.emojy.config.Emote
import com.mineinabyss.emojy.config.Gif
import net.kyori.adventure.key.Key
import org.slf4j.Logger
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.createDirectories
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

class ProxyEmoteRegistry(private val cacheDirectory: Path, private val logger: Logger) : EmoteRegistry {
    private data class ServerEmotes(val hash: String, val snapshot: EmotePayload.Snapshot)

    // Swapped as a whole so packets never see emotes and gifs from different rebuilds
    private class Merged(val emotes: Array<Emote>, val gifs: Array<Gif>, val spaceFont: Key)

    private val servers = ConcurrentHashMap<String, ServerEmotes>()

    @Volatile
    private var merged = Merged(emptyArray(), emptyArray(), Key.key("minecraft:space"))

    override val emotes get() = merged.emotes
    override val gifs get() = merged.gifs
    override val spaceFont get() = merged.spaceFont

    // Only consulted when escaping player input, which the backends already do
    override val requirePermissions = false

    fun hash(server: String) = servers[server]?.hash.orEmpty()

    fun accept(server: String, payload: ByteArray) {
        store(server, payload)
        runCatching { cacheDirectory.createDirectories().resolve(cacheFileName(server)).writeBytes(payload) }
            .onFailure { logger.warn("Failed to cache emotes of $server: ${it.message}") }
        rebuild()
        logger.info("Received ${servers.getValue(server).snapshot.emotes.size} emotes from $server")
    }

    // Backends only send their emotes once a player connects to them, too late for proxy plugins drawing the first tablist
    fun loadCache() {
        runCatching { cacheDirectory.listDirectoryEntries("*$CACHE_SUFFIX") }.getOrDefault(emptyList())
            .filter { it.isRegularFile() }
            .forEach { file ->
                runCatching { store(file.nameWithoutExtension, file.readBytes()) }
                    .onFailure { logger.warn("Failed to read cached emotes from ${file.fileName}: ${it.message}") }
            }
        rebuild()
        if (servers.isNotEmpty()) logger.info("Loaded cached emotes of ${servers.size} server(s)")
    }

    private fun store(server: String, payload: ByteArray) {
        servers[server] = ServerEmotes(EmotePayload.hash(payload), EmotePayload.decode(payload))
    }

    @Synchronized
    private fun rebuild() {
        val snapshots = servers.toSortedMap().values.map { it.snapshot }
        // Backends use the first emote with a given id, so later duplicates within one server are ignored
        val emotes = snapshots.flatMap { it.emotes.distinctBy(Emote::id) }

        emotes.groupBy(Emote::id)
            .filterValues { versions -> versions.distinctBy { it.font to it.unicodes }.size > 1 }
            .keys.forEach { logger.warn("Emote $it differs between servers, using the one from the last server by name") }

        merged = Merged(
            emotes = emotes.associateBy(Emote::id).values.toTypedArray(),
            gifs = snapshots.flatMap { it.gifs.distinctBy(Gif::id) }.associateBy(Gif::id).values.toTypedArray(),
            spaceFont = snapshots.lastOrNull()?.spaceFont ?: merged.spaceFont,
        )
    }

    private fun cacheFileName(server: String) = server.replace(UNSAFE_CHARACTERS, "_") + CACHE_SUFFIX

    private companion object {
        val UNSAFE_CHARACTERS = Regex("[^A-Za-z0-9._-]")
        const val CACHE_SUFFIX = ".json"
    }
}
