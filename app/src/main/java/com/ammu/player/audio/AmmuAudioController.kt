package com.ammu.player.audio

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.ammu.player.data.model.TrackEntity
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AbLoopState(
    val isEnabled: Boolean = false,
    val pointAMs: Long = 0L,
    val pointBMs: Long = 0L
)

@UnstableApi
class AmmuAudioController(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val _currentTrack = MutableStateFlow<TrackEntity?>(null)
    val currentTrack: StateFlow<TrackEntity?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _queue = MutableStateFlow<List<TrackEntity>>(emptyList())
    val queue: StateFlow<List<TrackEntity>> = _queue.asStateFlow()

    private val _currentQueueIndex = MutableStateFlow(-1)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()

    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _abLoopState = MutableStateFlow(AbLoopState())
    val abLoopState: StateFlow<AbLoopState> = _abLoopState.asStateFlow()

    private var progressJob: Job? = null
    var onTrackCompletedListener: ((TrackEntity) -> Unit)? = null

    init {
        initController()
        startProgressTracking()
    }

    private fun initController() {
        val sessionToken = SessionToken(context, ComponentName(context, AmmuMediaService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()?.apply {
                    addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            _isPlaying.value = isPlaying
                        }

                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_READY) {
                                _durationMs.value = duration.coerceAtLeast(0L)
                            } else if (playbackState == Player.STATE_ENDED) {
                                _currentTrack.value?.let { onTrackCompletedListener?.invoke(it) }
                                skipToNext()
                            }
                        }

                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            super.onMediaItemTransition(mediaItem, reason)
                            val idx = currentMediaItemIndex
                            _currentQueueIndex.value = idx
                            if (idx in _queue.value.indices) {
                                _currentTrack.value = _queue.value[idx]
                            }
                        }
                    })
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                mediaController?.let { controller ->
                    val pos = controller.currentPosition.coerceAtLeast(0L)
                    _positionMs.value = pos
                    val dur = controller.duration.coerceAtLeast(0L)
                    if (dur > 0L) {
                        _durationMs.value = dur
                    }

                    // Enforce A-B loop check
                    val loop = _abLoopState.value
                    if (loop.isEnabled && loop.pointBMs > loop.pointAMs && pos >= loop.pointBMs) {
                        controller.seekTo(loop.pointAMs)
                        _positionMs.value = loop.pointAMs
                    }
                }
                delay(150)
            }
        }
    }

    fun playTrack(track: TrackEntity, newQueue: List<TrackEntity>? = null) {
        val controller = mediaController ?: return

        val tracks = newQueue ?: _queue.value
        val trackIndex = tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)

        _queue.value = tracks
        _currentQueueIndex.value = trackIndex
        _currentTrack.value = track

        val mediaItems = tracks.map { t ->
            val uri = if (t.uriString.isNotBlank()) Uri.parse(t.uriString) else Uri.parse("file://${t.path}")
            MediaItem.Builder()
                .setMediaId(t.id.toString())
                .setUri(uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(t.displayTitle())
                        .setArtist(t.artist)
                        .setAlbumTitle(t.album)
                        .build()
                )
                .build()
        }

        controller.setMediaItems(mediaItems, trackIndex, 0L)
        controller.prepare()
        controller.play()
    }

    fun playPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun skipToNext() {
        val controller = mediaController ?: return
        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
        } else if (_repeatMode.value == Player.REPEAT_MODE_ALL && _queue.value.isNotEmpty()) {
            controller.seekTo(0, 0L)
        }
    }

    fun skipToPrevious() {
        val controller = mediaController ?: return
        if (controller.currentPosition > 3000L) {
            controller.seekTo(0L)
        } else if (controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _positionMs.value = positionMs
    }

    fun seekRelative(deltaMs: Long) {
        val controller = mediaController ?: return
        val target = (controller.currentPosition + deltaMs).coerceIn(0L, controller.duration.coerceAtLeast(0L))
        seekTo(target)
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        val newShuffle = !controller.shuffleModeEnabled
        controller.shuffleModeEnabled = newShuffle
        _isShuffleEnabled.value = newShuffle
    }

    fun toggleRepeatMode() {
        val controller = mediaController ?: return
        val nextMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        controller.repeatMode = nextMode
        _repeatMode.value = nextMode
    }

    fun setVolume(vol: Float) {
        _volume.value = vol.coerceIn(0f, 1f)
        mediaController?.volume = _volume.value
    }

    // A-B Loop Controls
    fun setAbLoopA(positionMs: Long) {
        _abLoopState.value = _abLoopState.value.copy(pointAMs = positionMs)
    }

    fun setAbLoopB(positionMs: Long) {
        _abLoopState.value = _abLoopState.value.copy(pointBMs = positionMs)
    }

    fun toggleAbLoop(enable: Boolean) {
        val s = _abLoopState.value
        if (enable && s.pointBMs > s.pointAMs) {
            _abLoopState.value = s.copy(isEnabled = true)
            mediaController?.seekTo(s.pointAMs)
        } else {
            _abLoopState.value = s.copy(isEnabled = false)
        }
    }

    fun clearAbLoop() {
        _abLoopState.value = AbLoopState()
    }

    // Queue Manipulation
    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in _queue.value.indices || toIndex !in _queue.value.indices) return
        val list = _queue.value.toMutableList()
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        _queue.value = list
        mediaController?.moveMediaItem(fromIndex, toIndex)
    }

    fun removeFromQueue(index: Int) {
        if (index !in _queue.value.indices) return
        val list = _queue.value.toMutableList()
        list.removeAt(index)
        _queue.value = list
        mediaController?.removeMediaItem(index)
    }

    fun release() {
        progressJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
    }
}
