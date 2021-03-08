package com.daasuu.epf.player;

import android.net.Uri;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.FormatHolder;
import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.source.MediaPeriod;
import com.google.android.exoplayer2.source.MediaSourceEventListener.EventDispatcher;
import com.google.android.exoplayer2.source.SampleStream;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.Loader;
import com.google.android.exoplayer2.upstream.StatsDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class ImageMediaPeriod implements MediaPeriod, Loader.Callback<ImageMediaPeriod.SourceLoadable> {

    private static final String TAG = "ljx-PicMediaPeriod";

    private static final Format format = Format.createImageSampleFormat(
            null,
            "image/jpeg",
            null,
            Format.NO_VALUE,
            C.SELECTION_FLAG_FORCED,
            null,
            null, null);


    private final ArrayList<ImageSampleStreamImpl> sampleStreams;
    private final TrackGroupArray tracks;
    private EventDispatcher eventDispatcher;
    private DataSource.Factory dataSourceFactory;
    /* package */ boolean notifiedReadingStarted;
    /* package */ boolean loadingFinished;
    /* package */ boolean loadingSucceeded;
    /* package */ byte[] sampleData;
    /* package */ int sampleSize;
    /* package */ final Loader loader;
    private final DataSpec dataSpec;
    private final TransferListener transferListener;
    private final long durationUs;
    private long startPositionUs;

    public ImageMediaPeriod(EventDispatcher eventDispatcher,
                            Uri uri, long durationUs, long startPositionUs,
                            DataSource.Factory dataSourceFactory,
                            TransferListener mediaTransferListener) {
        this.tracks = new TrackGroupArray(new TrackGroup(format));
        this.sampleStreams = new ArrayList<>();
        this.eventDispatcher = eventDispatcher;
        this.dataSourceFactory = dataSourceFactory;
        this.dataSpec = new DataSpec(uri, DataSpec.FLAG_ALLOW_GZIP);
        this.loader = new Loader("Loader:PicMediaPeriod");
        this.transferListener = mediaTransferListener;
        this.durationUs = durationUs;
        this.startPositionUs = startPositionUs;
        eventDispatcher.mediaPeriodCreated();
    }

    public void release() {
        loader.release();
        eventDispatcher.mediaPeriodReleased();
    }

    @Override
    public void prepare(Callback callback, long positionUs) {
        //Log.d(TAG, "prepare: " + Thread.currentThread().getName());
        callback.onPrepared(this);
    }


    @Override
    public void maybeThrowPrepareError() throws IOException {

    }

    @Override
    public TrackGroupArray getTrackGroups() {
        return tracks;
    }

    @Override
    public long selectTracks(TrackSelection[] selections, boolean[] mayRetainStreamFlags, SampleStream[] streams, boolean[] streamResetFlags, long positionUs) {
        String uri = dataSpec.uri.toString();
        Log.d(TAG, "selectTracks: " + Thread.currentThread().getName() + ", positionUs " + positionUs + ", uri " + uri.substring(uri.length() - 10, uri.length() - 4));
        for (int i = 0; i < selections.length; i++) {
            if (streams[i] != null && (selections[i] == null || !mayRetainStreamFlags[i])) {
                sampleStreams.remove(streams[i]);
                streams[i] = null;
            }
            if (streams[i] == null && selections[i] != null) {
                ImageSampleStreamImpl stream = new ImageSampleStreamImpl(dataSpec, durationUs);
                sampleStreams.add(stream);
                streams[i] = stream;
                streamResetFlags[i] = true;
            }
        }
        return positionUs;
    }

    @Override
    public void discardBuffer(long positionUs, boolean toKeyframe) {
        //Log.d(TAG, "discardBuffer: " + Thread.currentThread().getName());
    }

    @Override
    public void reevaluateBuffer(long positionUs) {
        //Log.d(TAG, "reevaluateBuffer: " + Thread.currentThread().getName());
    }

    @Override
    public boolean continueLoading(long positionUs) {
        //Log.d(TAG, "continueLoading: " + Thread.currentThread().getName());
        if (loadingFinished || loader.isLoading() || loader.hasFatalError()) {
            return false;
        }
        DataSource dataSource = dataSourceFactory.createDataSource();
        if (transferListener != null) {
            dataSource.addTransferListener(transferListener);
        }
        long elapsedRealtimeMs =
                loader.startLoading(
                        new SourceLoadable(dataSpec, dataSource),
                        /* callback= */ this,
                        0);
        eventDispatcher.loadStarted(
                dataSpec,
                C.DATA_TYPE_MEDIA,
                C.TRACK_TYPE_UNKNOWN,
                format,
                C.SELECTION_REASON_UNKNOWN,
                /* trackSelectionData= */ null,
                /* mediaStartTimeUs= */ 0,
                durationUs,
                elapsedRealtimeMs);
        return true;
    }

    @Override
    public long readDiscontinuity() {
        //Log.d(TAG, "readDiscontinuity: " + Thread.currentThread().getName());
        if (!notifiedReadingStarted) {
            eventDispatcher.readingStarted();
            notifiedReadingStarted = true;
        }
        return C.TIME_UNSET;
    }

    @Override
    public long seekToUs(long positionUs) {
        //Log.d(TAG, "seekToUs: " + Thread.currentThread().getName());
        for (int i = 0; i < sampleStreams.size(); i++) {
            sampleStreams.get(i).reset();
        }
        return positionUs;
    }

    @Override
    public long getAdjustedSeekPositionUs(long positionUs, SeekParameters seekParameters) {
        return positionUs;
    }

    /**
     * exoplayer的updatePeriods()中用来判断这个Period是否loading完成
     */
    @Override
    public long getBufferedPositionUs() {
        return loadingFinished ? C.TIME_END_OF_SOURCE : 0;
    }

    @Override
    public long getNextLoadPositionUs() {
        return loadingFinished || loader.isLoading() ? C.TIME_END_OF_SOURCE : 0;
    }

    @Override
    public void onLoadCompleted(SourceLoadable loadable, long elapsedRealtimeMs, long loadDurationMs) {
        //Log.d(TAG, "onLoadCompleted: ");
        sampleSize = (int) loadable.dataSource.getBytesRead();
        sampleData = loadable.sampleData;
        loadingFinished = true;
        loadingSucceeded = true;
        eventDispatcher.loadCompleted(
                loadable.dataSpec,
                loadable.dataSource.getLastOpenedUri(),
                loadable.dataSource.getLastResponseHeaders(),
                C.DATA_TYPE_MEDIA,
                C.TRACK_TYPE_UNKNOWN,
                format,
                C.SELECTION_REASON_UNKNOWN,
                /* trackSelectionData= */ null,
                /* mediaStartTimeUs= */ 0,
                durationUs,
                elapsedRealtimeMs,
                loadDurationMs,
                sampleSize);
    }

    @Override
    public void onLoadCanceled(SourceLoadable loadable, long elapsedRealtimeMs, long loadDurationMs, boolean released) {
        //Log.d(TAG, "onLoadCanceled: ");
        eventDispatcher.loadCanceled(
                loadable.dataSpec,
                loadable.dataSource.getLastOpenedUri(),
                loadable.dataSource.getLastResponseHeaders(),
                C.DATA_TYPE_MEDIA,
                C.TRACK_TYPE_UNKNOWN,
                /* trackFormat= */ null,
                C.SELECTION_REASON_UNKNOWN,
                /* trackSelectionData= */ null,
                /* mediaStartTimeUs= */ 0,
                durationUs,
                elapsedRealtimeMs,
                loadDurationMs,
                loadable.dataSource.getBytesRead());
    }

    @Override
    public Loader.LoadErrorAction onLoadError(SourceLoadable loadable, long elapsedRealtimeMs, long loadDurationMs, IOException error, int errorCount) {
        Log.e(TAG, "onLoadError: ", error);
        loadingFinished = true;

        eventDispatcher.loadError(
                loadable.dataSpec,
                loadable.dataSource.getLastOpenedUri(),
                loadable.dataSource.getLastResponseHeaders(),
                C.DATA_TYPE_MEDIA,
                C.TRACK_TYPE_UNKNOWN,
                format,
                C.SELECTION_REASON_UNKNOWN,
                /* trackSelectionData= */ null,
                /* mediaStartTimeUs= */ 0,
                durationUs,
                elapsedRealtimeMs,
                loadDurationMs,
                loadable.dataSource.getBytesRead(),
                error,
                /* wasCanceled= */ true);
        return Loader.DONT_RETRY_FATAL;
    }


    public class ImageSampleStreamImpl implements SampleStream {
        public DataSpec dataSpec;
        public long durationUs;
        public long startPositionUs;

        public ImageSampleStreamImpl(DataSpec dataSpec, long durationUs) {
            this.dataSpec = dataSpec;
            this.durationUs = durationUs;
            Log.d(TAG, "ImageSampleStreamImpl: " + dataSpec.uri + ", " + durationUs + ", " + startPositionUs);
        }

        public void reset() {
        }

        @Override
        public boolean isReady() {
            return loadingFinished;
        }

        @Override
        public void maybeThrowError() throws IOException {
        }

        @Override
        public int readData(FormatHolder formatHolder, DecoderInputBuffer buffer,
                            boolean requireFormat) {
            if (loadingFinished) {
                buffer.clear();
                if (loadingSucceeded) {
                    buffer.addFlag(C.BUFFER_FLAG_KEY_FRAME);
                    buffer.timeUs = 0;
                    if (buffer.isFlagsOnly()) {
                        return C.RESULT_BUFFER_READ;
                    }
                    buffer.ensureSpaceForWrite(sampleSize);
                    buffer.data.put(sampleData, 0, sampleSize);
                    buffer.addFlag(C.BUFFER_FLAG_END_OF_STREAM);
                }
                formatHolder.format = format;
                return C.RESULT_BUFFER_READ;
            }
            return C.RESULT_NOTHING_READ;
        }

        @Override
        public int skipData(long positionUs) {
            return 0;
        }

    }


    /* package */ static final class SourceLoadable implements Loader.Loadable {

        /**
         * The initial size of the allocation used to hold the sample data.
         */
        private static final int INITIAL_SAMPLE_SIZE = 1024;

        public final DataSpec dataSpec;

        private final StatsDataSource dataSource;

        private byte[] sampleData;

        public SourceLoadable(DataSpec dataSpec, DataSource dataSource) {
            this.dataSpec = dataSpec;
            this.dataSource = new StatsDataSource(dataSource);
        }

        @Override
        public void cancelLoad() {
            //Log.d(TAG, "cancelLoad: " + Thread.currentThread().getName());
            // Never happens.
        }

        @Override
        public void load() throws IOException, InterruptedException {
            //Log.d(TAG, "load: " + Thread.currentThread().getName());
            // We always load from the beginning, so reset bytesRead to 0.
            dataSource.resetBytesRead();
            try {
                // Create and open the input.
                dataSource.open(dataSpec);
                // Load the sample data.
                int result = 0;
                while (result != C.RESULT_END_OF_INPUT) {
                    int sampleSize = (int) dataSource.getBytesRead();
                    if (sampleData == null) {
                        sampleData = new byte[INITIAL_SAMPLE_SIZE];
                    } else if (sampleSize == sampleData.length) {
                        sampleData = Arrays.copyOf(sampleData, sampleData.length * 2);
                    }
                    result = dataSource.read(sampleData, sampleSize, sampleData.length - sampleSize);
                }
            } finally {
                Util.closeQuietly(dataSource);
            }
        }

    }
}
