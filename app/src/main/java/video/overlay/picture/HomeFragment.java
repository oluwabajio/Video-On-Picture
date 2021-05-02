package video.overlay.picture;

import android.content.Intent;
import android.graphics.PointF;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.linkedin.android.litr.MediaTransformer;
import com.linkedin.android.litr.TrackTransform;
import com.linkedin.android.litr.codec.MediaCodecDecoder;
import com.linkedin.android.litr.codec.MediaCodecEncoder;
import com.linkedin.android.litr.exception.MediaTransformationException;
import com.linkedin.android.litr.filter.GlFilter;
import com.linkedin.android.litr.filter.GlFrameRenderFilter;
import com.linkedin.android.litr.filter.Transform;
import com.linkedin.android.litr.filter.video.gl.DefaultVideoFrameRenderFilter;
import com.linkedin.android.litr.io.MediaExtractorMediaSource;
import com.linkedin.android.litr.io.MediaMuxerMediaTarget;
import com.linkedin.android.litr.io.MediaSource;
import com.linkedin.android.litr.io.MediaTarget;
import com.linkedin.android.litr.render.GlVideoRenderer;
import com.linkedin.android.litr.utils.CodecUtils;
import com.linkedin.android.litr.utils.TranscoderUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import video.overlay.picture.data.AudioTrackFormat;
import video.overlay.picture.data.GenericTrackFormat;
import video.overlay.picture.data.MediaTransformationListener;
import video.overlay.picture.data.SourceMedia;
import video.overlay.picture.data.TargetTrack;
import video.overlay.picture.data.TargetVideoConfiguration;
import video.overlay.picture.data.TransformationPresenter;
import video.overlay.picture.data.TransformationState;
import video.overlay.picture.data.VideoTrackFormat;
import video.overlay.picture.data.utils.TransformationUtil;
import video.overlay.picture.databinding.FragmentHomeBinding;

import static android.app.Activity.RESULT_OK;
import static android.media.MediaFormat.KEY_ROTATION;

public class HomeFragment extends Fragment {

    FragmentHomeBinding binding;
    int PICK_IMAGE = 101;
    int PICK_VIDEO = 102;
    private static final String TAG = "HomeFragment";
    private MediaTransformer mediaTransformer;
    private TargetMedia targetMedia;
    private SourceMedia sourceMedia ;
    TransformationState transformationState;
    TargetVideoConfiguration targetVideoConfiguration;
    TransformationPresenter transformationPresenter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        mediaTransformer = new MediaTransformer(getContext().getApplicationContext());
        targetMedia = new TargetMedia();

        initListeners();


        if (PermissionHelper.checkPermissions(getActivity())) {

        } else {
            PermissionHelper.requestPermissions(this);
        }


        sourceMedia = new SourceMedia();
        targetVideoConfiguration = new TargetVideoConfiguration();
        transformationState = new TransformationState();
        transformationPresenter = new TransformationPresenter(getContext(), mediaTransformer);


        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaTransformer.release();
    }

    private void initListeners() {
        binding.btnPickVideo.setOnClickListener(v -> pickVideo());
        binding.btnPickImage.setOnClickListener(v -> pickImage());
        binding.btnTranscode.setOnClickListener(v -> {
           startVideoOverlayTransformation(sourceMedia, targetMedia, targetVideoConfiguration, transformationState);
        });

        binding.btnPlay.setOnClickListener(v -> {
            Log.e(TAG, "initListeners: "+ transformationState.progress );

            if (transformationState.progress >= 100) {
//play(targetMedia.targetFile);
                Log.e(TAG, "initListeners: Path = "+ targetMedia.targetFile.getAbsolutePath() );
            }
        });
    }

    public void startVideoOverlayTransformation(@NonNull SourceMedia sourceMedia,
                                                @NonNull TargetMedia targetMedia,
                                                @NonNull TargetVideoConfiguration targetVideoConfiguration,
                                                @NonNull TransformationState transformationState) {
        if (targetMedia.getIncludedTrackCount() < 1) {
            return;
        }

        if (targetMedia.targetFile.exists()) {
            targetMedia.targetFile.delete();
        }

        transformationState.requestId = UUID.randomUUID().toString();
        MediaTransformationListener transformationListener = new MediaTransformationListener(getActivity(),
                transformationState.requestId,
                transformationState);

        try {
            MediaTarget mediaTarget = new MediaMuxerMediaTarget(targetMedia.targetFile.getPath(),
                    targetMedia.getIncludedTrackCount(),
                    targetVideoConfiguration.rotation,
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            List<TrackTransform> trackTransforms = new ArrayList<>(targetMedia.tracks.size());
            MediaSource mediaSource = new MediaExtractorMediaSource(getActivity(), sourceMedia.uri);

            for (TargetTrack targetTrack : targetMedia.tracks) {
                if (!targetTrack.shouldInclude) {
                    continue;
                }
                MediaFormat mediaFormat = createMediaFormat(targetTrack);
                if (mediaFormat != null && targetTrack.format instanceof VideoTrackFormat) {
                    mediaFormat.setInteger(KEY_ROTATION, targetVideoConfiguration.rotation);
                }
                TrackTransform.Builder trackTransformBuilder = new TrackTransform.Builder(mediaSource,
                        targetTrack.sourceTrackIndex,
                        mediaTarget)
                        .setTargetTrack(trackTransforms.size())
                        .setTargetFormat(mediaFormat)
                        .setEncoder(new MediaCodecEncoder())
                        .setDecoder(new MediaCodecDecoder());
                if (targetTrack.format instanceof VideoTrackFormat) {
                    // adding background bitmap first, to ensure that video renders on top of it
                    List<GlFilter> filters = new ArrayList<>();
                    if (targetMedia.backgroundImageUri != null) {
                        GlFilter backgroundImageFilter = TransformationUtil.createGlFilter(getActivity(),
                                targetMedia.backgroundImageUri,
                                new PointF(1, 1),
                                new PointF(0.5f, 0.5f),
                                0);
                        filters.add(backgroundImageFilter);
                    }

                    Transform transform = new Transform(new PointF(1f, 1f), new PointF(1f, 0.55f), 0);
                    GlFrameRenderFilter frameRenderFilter = new DefaultVideoFrameRenderFilter(transform);
                    filters.add(frameRenderFilter);

                    trackTransformBuilder.setRenderer(new GlVideoRenderer(filters));
                }

                trackTransforms.add(trackTransformBuilder.build());
            }

            mediaTransformer.transform(transformationState.requestId,
                    trackTransforms,
                    transformationListener,
                    MediaTransformer.GRANULARITY_DEFAULT);
        } catch (MediaTransformationException ex) {
            Log.e(TAG, "Exception when trying to perform track operation", ex);
        }
    }


    private void pickImage() {
        pickMedia("image/*", PICK_IMAGE);
    }

    private void pickVideo() {
        pickMedia("video/*", PICK_VIDEO);
    }


    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


//                NavHostFragment.findNavController(HomeFragment.this)
//                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data.getData() != null) {
            Log.e(TAG, "onActivityResult: " + data.getData());
            if (requestCode == PICK_IMAGE) {
                targetMedia.backgroundImageUri = data.getData();

            } else if (requestCode == PICK_VIDEO) {

                updateSourceMedia(sourceMedia, data.getData());
                File targetFile = new File(TransformationUtil.getTargetFileDirectory(),
                        "transcoded_" + TransformationUtil.getDisplayName(getContext(), data.getData()));
                targetMedia.setTargetFile(targetFile);
                targetMedia.setTracks(sourceMedia.tracks);

                transformationState.setState(TransformationState.STATE_IDLE);
                transformationState.setStats(null);
            }
        }
    }


    protected void updateSourceMedia(@NonNull SourceMedia sourceMedia, @NonNull Uri uri) {
        sourceMedia.uri = uri;
        sourceMedia.size = TranscoderUtils.getSize(getContext(), uri);
        sourceMedia.duration = getMediaDuration(uri) / 1000f;

        try {
            MediaExtractor mediaExtractor = new MediaExtractor();
            mediaExtractor.setDataSource(getContext(), uri, null);
            sourceMedia.tracks = new ArrayList<>(mediaExtractor.getTrackCount());

            for (int track = 0; track < mediaExtractor.getTrackCount(); track++) {
                MediaFormat mediaFormat = mediaExtractor.getTrackFormat(track);
                String mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mimeType == null) {
                    continue;
                }

                if (mimeType.startsWith("video")) {
                    VideoTrackFormat videoTrack = new VideoTrackFormat(track, mimeType);
                    videoTrack.width = getInt(mediaFormat, MediaFormat.KEY_WIDTH);
                    videoTrack.height = getInt(mediaFormat, MediaFormat.KEY_HEIGHT);
                    videoTrack.duration = getLong(mediaFormat, MediaFormat.KEY_DURATION);
                    videoTrack.frameRate = getInt(mediaFormat, MediaFormat.KEY_FRAME_RATE);
                    videoTrack.keyFrameInterval = getInt(mediaFormat, MediaFormat.KEY_I_FRAME_INTERVAL);
                    videoTrack.rotation = getInt(mediaFormat, KEY_ROTATION, 0);
                    videoTrack.bitrate = getInt(mediaFormat, MediaFormat.KEY_BIT_RATE);
                    sourceMedia.tracks.add(videoTrack);
                } else if (mimeType.startsWith("audio")) {
                    AudioTrackFormat audioTrack = new AudioTrackFormat(track, mimeType);
                    audioTrack.channelCount = getInt(mediaFormat, MediaFormat.KEY_CHANNEL_COUNT);
                    audioTrack.samplingRate = getInt(mediaFormat, MediaFormat.KEY_SAMPLE_RATE);
                    audioTrack.duration = getLong(mediaFormat, MediaFormat.KEY_DURATION);
                    audioTrack.bitrate = getInt(mediaFormat, MediaFormat.KEY_BIT_RATE);
                    sourceMedia.tracks.add(audioTrack);
                } else {
                    sourceMedia.tracks.add(new GenericTrackFormat(track, mimeType));
                }
            }
        } catch (IOException ex) {
            Log.e(TAG, "Failed to extract sourceMedia", ex);
        }


    }

    private void pickMedia(@NonNull String type, int pickMedia) {
        int PICK_MEDIA = pickMedia;

        Intent intent = new Intent();
        intent.setType(type);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        startActivityForResult(Intent.createChooser(intent, "Pick File"),
                PICK_MEDIA);
    }

    private int getInt(@NonNull MediaFormat mediaFormat, @NonNull String key) {
        return getInt(mediaFormat, key, -1);
    }

    private int getInt(@NonNull MediaFormat mediaFormat, @NonNull String key, int defaultValue) {
        if (mediaFormat.containsKey(key)) {
            return mediaFormat.getInteger(key);
        }
        return defaultValue;
    }

    private long getLong(@NonNull MediaFormat mediaFormat, @NonNull String key) {
        if (mediaFormat.containsKey(key)) {
            return mediaFormat.getLong(key);
        }
        return -1;
    }

    private MediaFormat createMediaFormat(@Nullable TargetTrack targetTrack) {
        MediaFormat mediaFormat = null;
        if (targetTrack != null && targetTrack.format != null) {
            mediaFormat = new MediaFormat();
            if (targetTrack.format.mimeType.startsWith("video")) {
                VideoTrackFormat trackFormat = (VideoTrackFormat) targetTrack.format;
                String mimeType = CodecUtils.MIME_TYPE_VIDEO_AVC;
                mediaFormat.setString(MediaFormat.KEY_MIME, mimeType);
                mediaFormat.setInteger(MediaFormat.KEY_WIDTH, trackFormat.width);
                mediaFormat.setInteger(MediaFormat.KEY_HEIGHT, trackFormat.height);
                mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, trackFormat.bitrate);
                mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, trackFormat.keyFrameInterval);
                mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, trackFormat.frameRate);
            } else if (targetTrack.format.mimeType.startsWith("audio")) {
                AudioTrackFormat trackFormat = (AudioTrackFormat) targetTrack.format;
                mediaFormat.setString(MediaFormat.KEY_MIME, trackFormat.mimeType);
                mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, trackFormat.channelCount);
                mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, trackFormat.samplingRate);
                mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, trackFormat.bitrate);
            }
        }

        return mediaFormat;
    }


    public void play(@Nullable File targetFile) {
        if (targetFile != null && targetFile.exists()) {
            Intent playIntent = new Intent(Intent.ACTION_VIEW);
            Uri videoUri = FileProvider.getUriForFile(getActivity(),
                    getActivity().getPackageName() + ".provider",
                    targetFile);
            playIntent.setDataAndType(videoUri, "video/*");
            playIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            getActivity().startActivity(playIntent);
        }
    }

    private long getMediaDuration(@NonNull Uri uri) {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(getContext(), uri);
        String durationStr = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        return Long.parseLong(durationStr);
    }
}