/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Environment
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.database.DatabaseProvider
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import com.metrolist.innertube.YouTube
import com.metrolist.music.constants.AudioQuality
import com.metrolist.music.constants.AudioQualityKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.FormatEntity
import com.metrolist.music.db.entities.SongEntity
import com.metrolist.music.di.DownloadCache
import com.metrolist.music.di.PlayerCache
import com.metrolist.music.utils.YTPlayerUtils
import com.metrolist.music.utils.enumPreference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadUtil
@Inject
constructor(
    @ApplicationContext context: Context,
    val database: MusicDatabase,
    val databaseProvider: DatabaseProvider,
    @DownloadCache val downloadCache: Cache,
    @PlayerCache val playerCache: Cache,
) {
    private val TAG = "DownloadUtil"
    private val connectivityManager = context.getSystemService<ConnectivityManager>()!!
    private val audioQuality by enumPreference(context, AudioQualityKey, AudioQuality.AUTO)
    private val songUrlCache = HashMap<String, Pair<String, Long>>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val downloads = MutableStateFlow<Map<String, Download>>(emptyMap())

    private val dataSourceFactory =
        ResolvingDataSource.Factory(
            CacheDataSource
                .Factory()
                .setCache(playerCache)
                .setUpstreamDataSourceFactory(
                    OkHttpDataSource.Factory(
                        OkHttpClient.Builder()
                            .proxy(YouTube.proxy)
                            .proxyAuthenticator { _, response ->
                                YouTube.proxyAuth?.let { auth ->
                                    response.request.newBuilder()
                                        .header("Proxy-Authorization", auth)
                                        .build()
                                } ?: response.request
                            }
                            .build(),
                    ),
                ),
        ) { dataSpec ->
            val mediaId = dataSpec.key ?: error("No media id")
            val length = if (dataSpec.length >= 0) dataSpec.length else 1

            if (playerCache.isCached(mediaId, dataSpec.position, length)) {
                return@Factory dataSpec
            }

            songUrlCache[mediaId]?.takeIf { it.second < System.currentTimeMillis() }?.let {
                return@Factory dataSpec.withUri(it.first.toUri())
            }

            val playbackData = runBlocking(Dispatchers.IO) {
                YTPlayerUtils.playerResponseForPlayback(
                    mediaId,
                    audioQuality = audioQuality,
                    connectivityManager = connectivityManager,
                )
            }.getOrThrow()
            val format = playbackData.format

            val actualContentLength = format.contentLength ?: run {
                var length: Long? = null
                val client = OkHttpClient.Builder()
                    .proxy(YouTube.proxy)
                    .proxyAuthenticator { _, response ->
                        YouTube.proxyAuth?.let { auth ->
                            response.request.newBuilder()
                                .header("Proxy-Authorization", auth)
                                .build()
                        } ?: response.request
                    }
                    .build()
                val request = okhttp3.Request.Builder()
                    .head()
                    .url(playbackData.streamUrl)
                    .build()
                client.newCall(request).execute().use { response ->
                    length = response.header("Content-Length")?.toLongOrNull()
                }
                length ?: error("Failed to retrieve content length")
            }

            database.query {
                upsert(
                    FormatEntity(
                        id = mediaId,
                        itag = format.itag,
                        mimeType = format.mimeType.split(";")[0],
                        codecs = format.mimeType.split("codecs=")[1].removeSurrounding("\""),
                        bitrate = format.bitrate,
                        sampleRate = format.audioSampleRate,
                        contentLength = actualContentLength,
                        loudnessDb = playbackData.audioConfig?.loudnessDb,
                        perceptualLoudnessDb = playbackData.audioConfig?.perceptualLoudnessDb,
                        playbackUrl = playbackData.playbackTracking?.videostatsPlaybackUrl?.baseUrl
                    ),
                )

                // Metadata registration only — dateDownload is intentionally NOT set here.
                // It belongs solely to onDownloadChanged()'s STATE_COMPLETED branch below,
                // which only fires once the download has actually finished. Setting it here
                // (at URL-resolve time, i.e. the moment the download merely *starts*) would
                // mark the song as "cached" before a single byte is written.
                val existing = getSongByIdBlocking(mediaId)?.song
                val updatedSong = existing ?: SongEntity(
                    id = mediaId,
                    title = playbackData.videoDetails?.title ?: "Unknown",
                    duration = playbackData.videoDetails?.lengthSeconds?.toIntOrNull() ?: 0,
                    thumbnailUrl = playbackData.videoDetails?.thumbnail?.thumbnails?.lastOrNull()?.url,
                    dateDownload = null,
                    isDownloaded = false
                )

                upsert(updatedSong)
            }

            val streamUrl = playbackData.streamUrl.let {
                "${it}&range=0-${actualContentLength}"
            }

            songUrlCache[mediaId] = streamUrl to playbackData.streamExpiresInSeconds * 1000L
            dataSpec.withUri(streamUrl.toUri())
        }

    val downloadNotificationHelper =
        DownloadNotificationHelper(context, ExoDownloadService.CHANNEL_ID)

    @OptIn(DelicateCoroutinesApi::class)
    val downloadManager: DownloadManager =
        DownloadManager(
            context,
            databaseProvider,
            downloadCache,
            dataSourceFactory,
            Executor(Runnable::run)
        ).apply {
            maxParallelDownloads = 3
            addListener(
                object : DownloadManager.Listener {
                    override fun onDownloadChanged(
                        downloadManager: DownloadManager,
                        download: Download,
                        finalException: Exception?,
                    ) {
                        downloads.update { map ->
                            map.toMutableMap().apply {
                                set(download.request.id, download)
                            }
                        }

                        scope.launch {
                            when (download.state) {
                                Download.STATE_COMPLETED -> {
                                    database.updateDownloadedInfo(download.request.id, true, LocalDateTime.now())
                                    runCatching {
                                        exportDownloadedAudioFile(context, download.request.id)
                                    }.onFailure { error ->
                                        Timber.tag(TAG).e(error, "Failed to export playable audio file for ${download.request.id}")
                                    }
                                }
                                Download.STATE_FAILED,
                                Download.STATE_STOPPED,
                                Download.STATE_REMOVING -> {
                                    database.updateDownloadedInfo(download.request.id, false, null)
                                }
                                else -> {
                                }
                            }
                        }
                    }

                    override fun onDownloadRemoved(
                        downloadManager: DownloadManager,
                        download: Download,
                    ) {
                        val downloadId = download.request.id

                        runCatching {
                            database.updateDownloadedInfo(downloadId, false, null)
                        }.onSuccess {
                            downloads.update { map ->
                                map.toMutableMap().apply {
                                    remove(downloadId)
                                }
                            }
                            Timber.tag(TAG).d("Successfully removed download $downloadId from in-memory map")
                        }.onFailure { error ->
                            Timber.tag(TAG).e(error, "Failed to update database for removed download $downloadId, keeping in-memory entry")
                        }
                    }
                }
            )
        }

    init {
        val result = mutableMapOf<String, Download>()
        val cursor = downloadManager.downloadIndex.getDownloads()
        while (cursor.moveToNext()) {
            result[cursor.download.request.id] = cursor.download
        }
        downloads.value = result
    }

    fun getDownload(songId: String): Flow<Download?> = downloads.map { it[songId] }

    /**
     * Copies a fully-downloaded song's audio bytes out of ExoPlayer's internal
     * download cache into a real, standalone audio file in the device's public
     * Music/Lovely Beats folder — visible immediately in the file manager and
     * playable by any other music/media app, not just this one.
     *
     * On Android 10+ this goes through MediaStore (the modern, permission-free
     * way to add files to shared storage). On Android 9 and below it falls back
     * to writing directly into the public Music directory, which needs the
     * WRITE_EXTERNAL_STORAGE permission requested at app startup.
     *
     * The downloaded stream (AAC/Opus) is decoded to raw PCM using Android's
     * built-in MediaCodec, then encoded to a genuine .mp3 file using the LAME
     * encoder (Android has no built-in MP3 encoder, so this is the only way
     * to produce a real, standalone playable .mp3).
     */
    private suspend fun exportDownloadedAudioFile(context: Context, mediaId: String) {
        val format = database.formatOnce(mediaId) ?: return
        val song = database.getSongByIdBlocking(mediaId) ?: return

        val safeTitle = song.song.title
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .trim()
            .ifEmpty { mediaId }
            .take(80)
        val fileName = "$safeTitle.mp3"
        val relativeFolder = "${Environment.DIRECTORY_MUSIC}/Lovely Beats"

        // Step 1: dump the fully-downloaded cached bytes into a temp container
        // file so MediaExtractor/MediaCodec can decode it.
        val containerExt = if (format.mimeType.contains("webm")) "webm" else "m4a"
        val tempContainer = File(context.cacheDir, "export_$mediaId.$containerExt")
        val cacheDataSource = CacheDataSource.Factory().setCache(downloadCache).createDataSource()
        val dataSpec = DataSpec.Builder()
            .setUri(Uri.EMPTY)
            .setKey(mediaId)
            .setLength(C.LENGTH_UNSET.toLong())
            .build()
        try {
            cacheDataSource.open(dataSpec)
            FileOutputStream(tempContainer).use { out ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = cacheDataSource.read(buffer, 0, buffer.size)
                    if (read == C.RESULT_END_OF_INPUT) break
                    out.write(buffer, 0, read)
                }
            }
        } finally {
            cacheDataSource.close()
        }

        try {
            val mp3Bytes = decodeToMp3(tempContainer)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Modern path: MediaStore. No storage permission required.
                val resolver = context.contentResolver
                val collection = android.provider.MediaStore.Audio.Media.getContentUri(
                    android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY,
                )

                val existsSelection = "${android.provider.MediaStore.Audio.Media.DISPLAY_NAME}=? AND " +
                    "${android.provider.MediaStore.Audio.Media.RELATIVE_PATH}=?"
                resolver.query(
                    collection,
                    arrayOf(android.provider.MediaStore.Audio.Media._ID),
                    existsSelection,
                    arrayOf(fileName, "$relativeFolder/"),
                    null,
                )?.use { cursor -> if (cursor.count > 0) return }

                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                    put(android.provider.MediaStore.Audio.Media.RELATIVE_PATH, relativeFolder)
                    put(android.provider.MediaStore.Audio.Media.IS_PENDING, 1)
                }

                val itemUri = resolver.insert(collection, values) ?: return
                try {
                    resolver.openOutputStream(itemUri)?.use { out -> out.write(mp3Bytes) }
                    values.clear()
                    values.put(android.provider.MediaStore.Audio.Media.IS_PENDING, 0)
                    resolver.update(itemUri, values, null, null)
                } catch (e: Exception) {
                    resolver.delete(itemUri, null, null)
                    throw e
                }
            } else {
                // Legacy path (Android 9 and below): requires WRITE_EXTERNAL_STORAGE,
                // requested at app startup in MainActivity.
                @Suppress("DEPRECATION")
                val musicDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                    "Lovely Beats",
                )
                if (!musicDir.exists()) musicDir.mkdirs()
                val outFile = File(musicDir, fileName)
                if (outFile.exists() && outFile.length() > 0) return

                FileOutputStream(outFile).use { it.write(mp3Bytes) }
                runCatching {
                    android.media.MediaScannerConnection.scanFile(
                        context,
                        arrayOf(outFile.absolutePath),
                        null,
                        null,
                    )
                }
            }
        } finally {
            tempContainer.delete()
        }
    }

    /**
     * Decodes an audio container file (m4a/webm) to raw PCM via MediaCodec,
     * then encodes that PCM to MP3 bytes via the LAME encoder.
     */
    private fun decodeToMp3(inputFile: File): ByteArray {
        val extractor = android.media.MediaExtractor()
        extractor.setDataSource(inputFile.absolutePath)

        var trackIndex = -1
        var trackFormat: android.media.MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            val mime = f.getString(android.media.MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                trackIndex = i
                trackFormat = f
                break
            }
        }
        require(trackIndex >= 0 && trackFormat != null) { "No audio track found in downloaded file" }
        extractor.selectTrack(trackIndex)

        val mime = trackFormat.getString(android.media.MediaFormat.KEY_MIME)!!
        val sampleRate = trackFormat.getInteger(android.media.MediaFormat.KEY_SAMPLE_RATE)
        val channelCount = trackFormat.getInteger(android.media.MediaFormat.KEY_CHANNEL_COUNT).coerceIn(1, 2)

        val codec = android.media.MediaCodec.createDecoderByType(mime)
        codec.configure(trackFormat, null, null, 0)
        codec.start()

        val lame = com.naman14.androidlame.LameBuilder()
            .setInSampleRate(sampleRate)
            .setOutChannels(channelCount)
            .setOutBitrate(192)
            .setOutSampleRate(sampleRate)
            .setQuality(3)
            .build()

        val mp3Output = java.io.ByteArrayOutputStream()
        val mp3Buffer = ByteArray(16 * 1024)
        val bufferInfo = android.media.MediaCodec.BufferInfo()
        var sawInputEOS = false
        var sawOutputEOS = false
        val timeoutUs = 10_000L

        try {
            while (!sawOutputEOS) {
                if (!sawInputEOS) {
                    val inIndex = codec.dequeueInputBuffer(timeoutUs)
                    if (inIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inIndex)!!
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            sawInputEOS = true
                        } else {
                            codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
                if (outIndex >= 0) {
                    if (bufferInfo.size > 0) {
                        val outputBuffer = codec.getOutputBuffer(outIndex)!!
                        val pcmBytes = ByteArray(bufferInfo.size)
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        outputBuffer.get(pcmBytes)

                        val shortBuffer = ShortArray(pcmBytes.size / 2)
                        java.nio.ByteBuffer.wrap(pcmBytes)
                            .order(java.nio.ByteOrder.LITTLE_ENDIAN)
                            .asShortBuffer()
                            .get(shortBuffer)

                        val samplesPerChannel = shortBuffer.size / channelCount
                        val encoded = lame.encodeBufferInterLeaved(shortBuffer, samplesPerChannel, mp3Buffer)
                        if (encoded > 0) mp3Output.write(mp3Buffer, 0, encoded)
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if (bufferInfo.flags and android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        sawOutputEOS = true
                    }
                }
            }

            val flushBuffer = ByteArray(7200)
            val flushed = lame.flush(flushBuffer)
            if (flushed > 0) mp3Output.write(flushBuffer, 0, flushed)
        } finally {
            runCatching { lame.close() }
            codec.stop()
            codec.release()
            extractor.release()
        }

        return mp3Output.toByteArray()
    }

    fun release() {
        scope.cancel()
    }
}
