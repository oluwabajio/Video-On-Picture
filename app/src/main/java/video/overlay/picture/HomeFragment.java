package video.overlay.picture;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.hbisoft.pickit.PickiT;
import com.hbisoft.pickit.PickiTCallbacks;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import video.overlay.picture.utils.MediaUtils;

import static android.app.Activity.RESULT_OK;
import static android.media.MediaFormat.KEY_ROTATION;

public class HomeFragment extends Fragment {

    FragmentHomeBinding binding;
    int PICK_IMAGE = 101;
    int PICK_VIDEO = 102;
    private static final String TAG = "HomeFragment";
    private TargetMedia targetMedia;
    private SourceMedia sourceMedia;
    TransformationState transformationState;
    TargetVideoConfiguration targetVideoConfiguration;
    String imageFilePath;
    String videoFilePath;
    PickiT pickiT;
    String pickitProcess = "";
    String RCode;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        initListeners();
        requestPermission();
        targetMedia = new TargetMedia();
        sourceMedia = new SourceMedia();
        pickiT = new PickiT(getActivity(), pickiTCallbacks, getActivity());
        targetVideoConfiguration = new TargetVideoConfiguration();
        transformationState = new TransformationState();

        initAds();
        return view;
    }

    private void initAds() {
        MobileAds.initialize(getActivity(), new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        AdRequest adRequest = new AdRequest.Builder().build();
        binding.adView.loadAd(adRequest);

    }

    private void requestPermission() {
        if (!PermissionHelper.checkPermissions(getActivity())) {
            PermissionHelper.requestPermissions(this);
        }
    }

    private void initListeners() {
        binding.btnPickVideo.setOnClickListener(v -> {
            selectVideo();
        });
        binding.btnPickImage.setOnClickListener(v -> {
            selectImage();
        });


        binding.btnNext.setOnClickListener(v -> {
            if ( TextUtils.isEmpty(imageFilePath)) {
                Toast.makeText(getActivity(), "Kindly Select an Image File", Toast.LENGTH_LONG).show();
                return;
            }

            if (TextUtils.isEmpty(videoFilePath)) {
                Toast.makeText(getActivity(), "Kindly Select a Video File", Toast.LENGTH_LONG).show();
                return;
            }


            Bundle bundle = new Bundle();
            bundle.putString("video", videoFilePath);
            bundle.putString("image", imageFilePath);
            MediaUtils.getInstance().setTargetMedia(targetMedia);
            MediaUtils.getInstance().setSourceMedia(sourceMedia);
            MediaUtils.getInstance().setTargetVideoConfiguration(targetVideoConfiguration);
            MediaUtils.getInstance().setTransformationState(transformationState);
            Navigation.findNavController(getView()).navigate(R.id.action_HomeFragment_to_PlayerFragment, bundle);
        });

    }

    private void selectImage() {
        if (PermissionHelper.checkPermissions(getActivity())) {
            pickImage();
        } else {
            PermissionHelper.requestPermissions(this);
        }
    }

    private void selectVideo() {
        if (PermissionHelper.checkPermissions(getActivity())) {
            pickVideo();
        } else {
            PermissionHelper.requestPermissions(this);
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
                pickitProcess = "image";
                pickiT.getPath(data.getData(), Build.VERSION.SDK_INT);


                targetMedia.backgroundImageUri = data.getData();
                binding.imgPicture.setImageURI(data.getData());

            } else if (requestCode == PICK_VIDEO) {
                pickitProcess = "video";
                pickiT.getPath(data.getData(), Build.VERSION.SDK_INT);

                Bitmap bitmap = BitmapFactory.decodeFile(imageFilePath);


                updateSourceMedia(sourceMedia, data.getData());
                File targetFile = new File(TransformationUtil.getTargetFileDirectory(), "VideoOnPicture_" + new SimpleDateFormat("yyyyMM_dd-HHmmss").format(new Date())+".mp4");
                targetMedia.setTargetFile(targetFile);
                targetMedia.setTracks(sourceMedia.tracks, bitmap.getWidth(), bitmap.getHeight());


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
        startActivityForResult(Intent.createChooser(intent, "Pick File"), PICK_MEDIA);
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


    PickiTCallbacks pickiTCallbacks = new PickiTCallbacks() {
        @Override
        public void PickiTonUriReturned() {
        }

        @Override
        public void PickiTonStartListener() {
        }

        @Override
        public void PickiTonProgressUpdate(int progress) {
        }

        @Override
        public void PickiTonCompleteListener(String path, boolean wasDriveFile, boolean wasUnknownProvider, boolean wasSuccessful, String Reason) {
            //   Toast.makeText(getActivity(), "Real Path = " + path, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "PickiTonCompleteListener: Real Path = " + path);
            Log.e(TAG, "PickiTonCompleteListener: Real Path = " + pickitProcess);

            if (pickitProcess.equalsIgnoreCase("image")) {
                imageFilePath = path;
                Log.e(TAG, "PickiTonCompleteListener: got to image");
                Bitmap bitmap = BitmapFactory.decodeFile(imageFilePath);
                Log.e(TAG, "PickiTonCompleteListener: width = " + bitmap.getWidth() + " height = " + bitmap.getHeight());
            } else if (pickitProcess.equalsIgnoreCase("video")) {
                videoFilePath = path;
                Bitmap bMap = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.MICRO_KIND);
                binding.imgVideo.setImageBitmap(bMap);
                Log.e(TAG, "PickiTonCompleteListener: got to video");
            }

            if (!TextUtils.isEmpty(imageFilePath) && !TextUtils.isEmpty(videoFilePath)) {
                binding.btnNext.setVisibility(View.VISIBLE);
            }

        }
    };

    private long getMediaDuration(@NonNull Uri uri) {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(getContext(), uri);
        String durationStr = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        return Long.parseLong(durationStr);
    }
}