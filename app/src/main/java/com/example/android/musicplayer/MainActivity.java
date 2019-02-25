package com.example.android.musicplayer;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    MediaPlayer mMediaPlayer;
    // volume seek bar to control the volume
    private SeekBar volumeSeekbar = null;
    // progress seek bar to control/show the current play position of the audio file
    private SeekBar progressSeekbar = null;
    private AudioManager mAudioManager = null;
    // duration of the audio file
    private int duration;
    // Handling a new thread to update the progress seekbar
    private Handler mHandler;


    // Listening to audio focus to handle any loss of it
    private AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
                public void onAudioFocusChange(int focusChange) {
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        mMediaPlayer.pause();
                    } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                        releaseResources();

                    } else if (focusChange ==
                            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                        // Do nothing
                    } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                        // Regain audio focus
                        mMediaPlayer.start();
                    }
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // finding the Progress bar from the xml file
        progressSeekbar = findViewById(R.id.progress_seekbar);

        // Establishing MediaPlayer object
        setupMediaPlayer();

        // Listening to Play button
        Button playBtn = findViewById(R.id.play_button);
        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mMediaPlayer == null) {

                    // request audio focus
                    int request = mAudioManager.requestAudioFocus(mOnAudioFocusChangeListener,
                            AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                    if (request == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        setupMediaPlayer();
                    }
                }

                // return if the media player is already playing the audio file
                if (mMediaPlayer.isPlaying()) {
                    Toast.makeText(MainActivity.this, "Already playing a file", Toast.LENGTH_SHORT).show();
                    return;
                }

                // play the track
                mMediaPlayer.start();

                // engage the progress bar
                updatingProgressBar();

                // listening to when the media file finishes playing
                mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        Toast.makeText(MainActivity.this, "It's done", Toast.LENGTH_SHORT).show();
                        releaseResources();
                    }
                });

            }
        });

        // Listening to pause button
        Button pauseBtn = findViewById(R.id.pause_button);
        pauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMediaPlayer.pause();
                releaseProgressBarHandler();
            }
        });

        // Listening to stop button
        Button stopBtn = findViewById(R.id.stop_button);
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // stop the playing to the beginning
                mMediaPlayer.stop();
                // reset stuff after stopping the play
                releaseResources();
            }
        });

        // Controlling the volume
        volumeControl();

        // Disable dragging the progress seekbar by default
        // progressSeekbar.setEnabled(false);
        // Listening to the progress bar drags
        progressSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mMediaPlayer != null && fromUser) {
                    mMediaPlayer.seekTo(progress);
                }
            }
        });

    }

    private void setupMediaPlayer() {
        if (mMediaPlayer == null) {
            // add track file
            mMediaPlayer = MediaPlayer.create(MainActivity.this, R.raw.alnas);
            // Getting track length
            duration = mMediaPlayer.getDuration();
            //  Log.i("mediaplayer", "media player duration: " + duration);

            // enable using the seekbar
            progressSeekbar.setEnabled(true);
            // setting the max value of the progress bar to the duration of the played audio file
            progressSeekbar.setMax(duration);
        }
    }


    /***/
    private void releaseResources() {

        // release media player resources & abandon audio focus
        releaseMediaPlayer();

        // setting the progress seekbar to the beginning
        progressSeekbar.setProgress(0);

        // Disable dragging the progress seekbar by default
        progressSeekbar.setEnabled(false);

        // stop the progress bar mHandler thread
        releaseProgressBarHandler();
    }

    /**
     * controlling volume of the audio file
     */
    private void volumeControl() {
        try {
            volumeSeekbar = findViewById(R.id.volume_seekbar);
            mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            volumeSeekbar.setMax(mAudioManager
                    .getStreamMaxVolume(AudioManager.STREAM_MUSIC));
            volumeSeekbar.setProgress(mAudioManager
                    .getStreamVolume(AudioManager.STREAM_MUSIC));


            volumeSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
                }

            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * Updating the progress bar when the audio file continues to play
     */
    private void updatingProgressBar() {
        mHandler = new Handler();
        //Make sure you update Seekbar on UI thread
        MainActivity.this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (mMediaPlayer != null) {
                    int mCurrentPosition = mMediaPlayer.getCurrentPosition();
                    // Log.i("progressbar", "Current position in progress bar: " + mCurrentPosition);
                    progressSeekbar.setProgress(mCurrentPosition);
                }

                // start the mHandler to update the progress bar every 1 sec by running this thread every 1000 milliseconds
                mHandler.postDelayed(this, 1000);
            }
        });
    }

    private void releaseProgressBarHandler() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    private void releaseMediaPlayer() {
        mMediaPlayer.release();
        mMediaPlayer = null;

        // Regardless of whether or not we were granted audio focus, abandon it. This also unregisters the AudioFocusChangeListener so we don't get anymore callbacks.
        mAudioManager.abandonAudioFocus(mOnAudioFocusChangeListener);
    }


}

