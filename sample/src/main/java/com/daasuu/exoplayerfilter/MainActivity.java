package com.daasuu.exoplayerfilter;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.daasuu.epf.EPlayerView;
import com.daasuu.epf.player.CustomRenderersFactory;
import com.daasuu.epf.player.ImageMediaSource;
import com.daasuu.epf.player.ImageRenderer;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.List;

import static android.content.Intent.EXTRA_ALLOW_MULTIPLE;
import static com.google.android.exoplayer2.C.SELECTION_FLAG_FORCED;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private EPlayerView ePlayerView;
    private SimpleExoPlayer player;
    private Button button;
    private SeekBar seekBar;
    private PlayerTimer playerTimer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUpViews();

        setUpSimpleExoPlayer();
        setUoGlPlayerView();
        setUpTimer();

        findViewById(R.id.btn_file).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectFile();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
        if (playerTimer != null) {
            playerTimer.stop();
            playerTimer.removeMessages(0);
        }
    }

    private void setUpViews() {
        // play pause
        button = (Button) findViewById(R.id.btn);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player == null) return;

                if (button.getText().toString().equals(MainActivity.this.getString(R.string.pause))) {
                    player.setPlayWhenReady(false);
                    button.setText(R.string.play);
                } else {
                    player.setPlayWhenReady(true);
                    button.setText(R.string.pause);
                }
            }
        });

        // seek
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (player == null) return;

                if (!fromUser) {
                    // We're not interested in programmatically generated changes to
                    // the progress bar's position.
                    return;
                }

                player.seekTo(progress * 1000);
                Log.d(TAG, "onProgressChanged: ");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // do nothing
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // do nothing
            }
        });

        // list
        ListView listView = (ListView) findViewById(R.id.list);
        final List<FilterType> filterTypes = FilterType.createFilterList();
        listView.setAdapter(new FilterAdapter(this, R.layout.row_text, filterTypes));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ePlayerView.setGlFilter(FilterType.createGlFilter(filterTypes.get(position), getApplicationContext()));
            }
        });
//        SurfaceView surfaceView = findViewById(R.id.surfaceview);
//        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
//            @Override
//            public void surfaceCreated(SurfaceHolder holder) {
//                final Surface surface = holder.getSurface();
//                ImageRenderer.drawBitmapTOSurface(surface, ((BitmapDrawable) getResources().getDrawable(R.drawable.aaa)).getBitmap());
//            }
//
//            @Override
//            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//
//            }
//
//            @Override
//            public void surfaceDestroyed(SurfaceHolder holder) {
//
//            }
//        });
    }

    private static int getDefaultBufferSize(int trackType) {
        switch (trackType) {
            case C.TRACK_TYPE_DEFAULT:
                return DefaultLoadControl.DEFAULT_MUXED_BUFFER_SIZE;
            case C.TRACK_TYPE_AUDIO:
                return DefaultLoadControl.DEFAULT_AUDIO_BUFFER_SIZE;
            case C.TRACK_TYPE_VIDEO:
                return DefaultLoadControl.DEFAULT_VIDEO_BUFFER_SIZE;
            case C.TRACK_TYPE_TEXT:
                return DefaultLoadControl.DEFAULT_TEXT_BUFFER_SIZE;
            case C.TRACK_TYPE_METADATA:
                return DefaultLoadControl.DEFAULT_METADATA_BUFFER_SIZE;
            case C.TRACK_TYPE_CAMERA_MOTION:
                return DefaultLoadControl.DEFAULT_CAMERA_MOTION_BUFFER_SIZE;
            case C.TRACK_TYPE_NONE:
                return 0;
            default:
                throw new IllegalArgumentException();
        }
    }


    private void setUpSimpleExoPlayer() {
        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "yourApplicationName"));
//        Constant.STREAM_URL_MP4_VOD_SHORT
        // This is the MediaSource representing the media to be played.
        MediaSource videoSource1 = new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(Uri.parse("http://10.255.206.106:81/mv.mp4"));
        MediaSource videoSource2 = new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(Uri.parse("http://10.255.206.106:81/video/111.mp4"));

//        MediaSource videoSource2 = new ProgressiveMediaSource.Factory(dataSourceFactory)
//                .createMediaSource(Uri.parse(Constant.STREAM_URL_MP4_VOD_SHORT));
        MediaSource videoSource = new ConcatenatingMediaSource(videoSource2, videoSource1);
        // SimpleExoPlayer
//        player = ExoPlayerFactory.newSimpleInstance(this);

        LoadControl loadControl = new DefaultLoadControl() {
            @Override
            protected int calculateTargetBufferSize(Renderer[] renderers, TrackSelectionArray trackSelectionArray) {
                int targetBufferSize = 0;
                for (int i = 0; i < renderers.length; i++) {
                    if (trackSelectionArray.get(i) != null) {
                        targetBufferSize += getDefaultBufferSize(renderers[i].getTrackType());
                    }
                }
                return targetBufferSize;
            }
        };
        player = ExoPlayerFactory.newSimpleInstance(this, new CustomRenderersFactory(this), new DefaultTrackSelector(), loadControl);
//        SurfaceView surfaceView = findViewById(R.id.surfaceview);
//        player.setVideoSurfaceView(surfaceView);
        player.setSeekParameters(new SeekParameters(0, 1000 * 1000));
        // Prepare the player with the source.
        player.prepare(videoSource1);
        player.setPlayWhenReady(true);

    }

    private void selectFile() {
        //调用系统文件管理器打开指定路径目录
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, 1111);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: ");
        if (requestCode == 1111) {
            // Produces DataSource instances through which media data is loaded.
            DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "yourApplicationName"));
            // This is the MediaSource representing the media to be played.
            Format format = Format.createImageSampleFormat(
                    null,
                    "image/jpeg",
                    null,
                    Format.NO_VALUE,
                    SELECTION_FLAG_FORCED,
                    null,
                    null, null);

            Uri fileUri = data == null ? null : data.getData();
            if (fileUri != null) {
//                MediaSource imageSource = new ImageMediaSource(dataSourceFactory, fileUri, 2_000_000);
//                MediaSource videoSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse("http://10.255.207.16:81/mv.mp4"));
//                ConcatenatingMediaSource source = new ConcatenatingMediaSource(true, imageSource, videoSource);
                MediaSource source = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(fileUri);
                player.prepare(source);
                player.setPlayWhenReady(true);

            } else if (data.getClipData() != null) {
                MediaSource[] sources = new MediaSource[data.getClipData().getItemCount() + 1];
                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    Uri uri = data.getClipData().getItemAt(i).getUri();
//                    int j = i >= 2 ? i + 1 : i;
                    int j = i >= 100 ? i + 1 : i;
                    MediaSource imageSource = new ImageMediaSource(dataSourceFactory, uri, 2_000_000);
//                MediaSource imageSource = new SingleSampleMediaSource.Factory(dataSourceFactory).createMediaSource(uri, format, 2_000_000);
                    sources[j] = imageSource;
                }
//            MediaSource imageSource = new SingleSampleMediaSource.Factory(dataSourceFactory).createMediaSource(fileUri, format, 2_000_000);
//            MediaSource imageSource1 = new SingleSampleMediaSource.Factory(dataSourceFactory).createMediaSource(image1, format, 2_000_000);
//                MediaSource imageSource = new ImageMediaSource(dataSourceFactory, fileUri, 2_000_000);
                MediaSource videoSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse("http://10.255.207.16:81/mv.mp4"));
                sources[sources.length - 1] = videoSource;
//                sources[2] = videoSource;
//                ConcatenatingMediaSource source = new ConcatenatingMediaSource(true, videoSource, imageSource);
                ConcatenatingMediaSource source = new ConcatenatingMediaSource(true, sources);
                player.prepare(source);
                player.setPlayWhenReady(true);
            }
        }
    }

    private void setUoGlPlayerView() {
        ePlayerView = new EPlayerView(this);
        ePlayerView.setSimpleExoPlayer(player);
        ePlayerView.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        ((MovieWrapperView) findViewById(R.id.layout_movie_wrapper)).addView(ePlayerView);
        ePlayerView.onResume();
    }


    private void setUpTimer() {
        playerTimer = new PlayerTimer();
        playerTimer.setCallback(new PlayerTimer.Callback() {
            @Override
            public void onTick(long timeMillis) {
                long position = player.getCurrentPosition();
                long duration = player.getDuration();

                if (duration <= 0) return;

                seekBar.setMax((int) duration / 1000);
                seekBar.setProgress((int) position / 1000);
            }
        });
        playerTimer.start();
    }


    private void releasePlayer() {
        ePlayerView.onPause();
        ((MovieWrapperView) findViewById(R.id.layout_movie_wrapper)).removeAllViews();
        ePlayerView = null;
        player.stop();
        player.release();
        player = null;
    }


}