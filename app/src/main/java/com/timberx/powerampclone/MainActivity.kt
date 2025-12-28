package com.timberx.powerampclone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.timberx.powerampclone.library.MediaStoreScanner
import com.timberx.powerampclone.model.Track
import com.timberx.powerampclone.player.AudioPlayer
import com.timberx.powerampclone.playlist.PlaylistManager
import com.timberx.powerampclone.service.PlayerService
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var recycler: RecyclerView
    private lateinit var audioPlayer: AudioPlayer

    private val permissionToRequest: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) loadLibrary()
            else {
                // Could show rationale / fallback
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recycler = findViewById(R.id.recyclerTracks)
        recycler.layoutManager = LinearLayoutManager(this)
        audioPlayer = AudioPlayer.getInstance(this)

        // Request runtime permission depending on SDK
        if (ContextCompat.checkSelfPermission(this, permissionToRequest)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(permissionToRequest)
        } else {
            loadLibrary()
        }

        // Start service (foreground service will promote itself when needed)
        val svcIntent = Intent(this, PlayerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svcIntent)
        } else {
            startService(svcIntent)
        }
    }

    private fun loadLibrary() {
        lifecycleScope.launch {
            val tracks: List<Track> = MediaStoreScanner.queryAudio(this@MainActivity)
            if (tracks.isEmpty()) {
                // Optionally show empty state
            }
            PlaylistManager.setPlaylist(tracks)
            recycler.adapter = SimpleTrackAdapter(tracks) { track ->
                PlaylistManager.setIndex(tracks.indexOf(track))
                audioPlayer.playTrack(track)
            }
        }
    }
}
