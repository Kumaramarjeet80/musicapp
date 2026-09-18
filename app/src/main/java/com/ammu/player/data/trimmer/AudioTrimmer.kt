package com.ammu.player.data.trimmer

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import com.ammu.player.data.db.AmmuDatabase
import com.ammu.player.data.model.AuditLogEntity
import com.ammu.player.data.model.TrimmedClipEntity
import java.io.File
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AudioTrimmer(
    private val context: Context,
    private val db: AmmuDatabase
) {
    /**
     * Slices an audio track from [startMs] to [endMs] using MediaExtractor and MediaMuxer.
     * Persists the resulting clip to the app's internal clips storage and updates Room database.
     */
    suspend fun trimAudio(
        parentTrackId: Long,
        sourcePathOrUri: String,
        clipTitle: String,
        startMs: Long,
        endMs: Long
    ): Result<TrimmedClipEntity> = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        try {
            if (sourcePathOrUri.startsWith("content://")) {
                extractor.setDataSource(context, Uri.parse(sourcePathOrUri), null)
            } else {
                extractor.setDataSource(sourcePathOrUri)
            }

            // Find audio track
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex < 0 || audioFormat == null) {
                return@withContext Result.failure(IllegalStateException("No valid audio stream found in source"))
            }

            extractor.selectTrack(audioTrackIndex)

            // Prepare destination file
            val clipsDir = File(context.filesDir, "trimmed_clips").apply { mkdirs() }
            val cleanTitle = clipTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val outputFile = File(clipsDir, "clip_${cleanTitle}_${System.currentTimeMillis()}.mp4")

            // Initialize Muxer
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerAudioTrack = muxer.addTrack(audioFormat)
            muxer.start()

            // Seek extractor to startMs
            val startUs = startMs * 1000L
            val endUs = endMs * 1000L
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            val maxBufferSize = if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            } else {
                1024 * 512 // 512KB fallback
            }
            val buffer = ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) {
                    bufferInfo.size = 0
                    break
                }

                bufferInfo.presentationTimeUs = extractor.sampleTime
                if (bufferInfo.presentationTimeUs > endUs) {
                    break
                }

                if (bufferInfo.presentationTimeUs >= startUs) {
                    bufferInfo.flags = extractor.sampleFlags
                    // Adjust presentation timestamp relative to start
                    bufferInfo.presentationTimeUs -= startUs
                    muxer.writeSampleData(muxerAudioTrack, buffer, bufferInfo)
                }

                if (!extractor.advance()) {
                    break
                }
            }

            muxer.stop()
            muxer.release()
            muxer = null
            extractor.release()

            val clipEntity = TrimmedClipEntity(
                parentTrackId = parentTrackId,
                clipTitle = clipTitle,
                startMs = startMs,
                endMs = endMs,
                clipPath = outputFile.absolutePath,
                clipSizeBytes = outputFile.length(),
                createdAt = System.currentTimeMillis()
            )

            val newId = db.trimmedClipDao().insertClip(clipEntity)
            val persistedClip = clipEntity.copy(id = newId)

            db.auditLogDao().insertLog(
                AuditLogEntity(
                    actionType = "AUDIO_TRIM_CLIP_CREATED",
                    details = "Created clip '${clipTitle}' [${startMs}ms - ${endMs}ms] size: ${outputFile.length()} bytes",
                    affectedEntityId = newId.toString(),
                    status = "SUCCESS"
                )
            )

            Result.success(persistedClip)
        } catch (e: Exception) {
            muxer?.runCatching {
                stop()
                release()
            }
            extractor.runCatching { release() }
            Result.failure(e)
        }
    }
}
