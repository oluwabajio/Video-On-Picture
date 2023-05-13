package video.overlay.picture;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import me.littlecheesecake.croplayout.EditPhotoView;
import me.littlecheesecake.croplayout.EditableImage;
import me.littlecheesecake.croplayout.handler.OnBoxChangedListener;
import me.littlecheesecake.croplayout.model.ScalableBox;
import video.overlay.picture.data.AudioTrackFormat;
import video.overlay.picture.data.MediaTransformationListener;
import video.overlay.picture.data.SourceMedia;
import video.overlay.picture.data.TargetTrack;
import video.overlay.picture.data.TargetVideoConfiguration;
import video.overlay.picture.data.TransformationState;
import video.overlay.picture.data.VideoTrackFormat;
import video.overlay.picture.data.utils.TransformationUtil;
import video.overlay.picture.databinding.FragmentPlayerBinding;
import video.overlay.picture.utils.MediaUtils;

import static android.media.MediaFormat.KEY_ROTATION;

public class PlayerFragment extends Fragment {

    FragmentPlayerBinding binding;

    String videoFilePath;
    String imageFilePath;
    SourceMedia sourceMedia;
    TargetMedia targetMedia;
    TargetVideoConfiguration targetVideoConfiguration;
    TransformationState transformationState;
    private static final String TAG = "PlayerFragment";
    private MediaTransformer mediaTransformer;
    int xx1, yy1, xx2, yy2;

    PointF position;
    PointF dimensions;

    private InterstitialAd mInterstitialAd;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentPlayerBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        getBundleFromIntent();
        initAreaSelection();
        initListener();
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
        InterstitialAd.load(getActivity(),getActivity().getString(R.string.admob_interstitial), adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                // The mInterstitialAd reference will be null until
                // an ad is loaded.
                mInterstitialAd = interstitialAd;
                Log.i(TAG, "onAdLoaded");
                mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        // Called when fullscreen content is dismissed.
                        Log.d("TAG", "The ad was dismissed.");
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                        // Called when fullscreen content failed to show.
                        Log.d("TAG", "The ad failed to show.");
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        // Called when fullscreen content is shown.
                        // Make sure to set your reference to null so you don't
                        // show it a second time.
                        mInterstitialAd = null;
                        Log.d("TAG", "The ad was shown.");
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                // Handle the error
                Log.i(TAG, loadAdError.getMessage());
                mInterstitialAd = null;
            }
        });



    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaTransformer.release();
    }

    private void getBundleFromIntent() {
        mediaTransformer = new MediaTransformer(getContext().getApplicationContext());
        imageFilePath = getArguments().getString("image");
        videoFilePath = getArguments().getString("video");
        sourceMedia = MediaUtils.getInstance().getSourceMedia();
        targetMedia = MediaUtils.getInstance().getTargetMedia();
        targetVideoConfiguration = MediaUtils.getInstance().getTargetVideoConfiguration();
        transformationState = MediaUtils.getInstance().getTransformationState();
    }

    private void initListener() {
        binding.btnTranscode.setOnClickListener(v -> {
            if (isValidPositionAndDimension()) {
                startVideoOverlayTransformation(sourceMedia, targetMedia, targetVideoConfiguration, transformationState);
            }
        });

        binding.btnPlay.setOnClickListener(v -> {

            if (transformationState.progress >= 100) {
               play(targetMedia.targetFile);

            }


        });

    }


    public void play(@Nullable File targetFile) {
        if (targetFile != null && targetFile.exists()) {
            Intent playIntent = new Intent(Intent.ACTION_VIEW);
            Uri videoUri = FileProvider.getUriForFile(getActivity(), getActivity().getPackageName() + ".provider", targetFile);
            playIntent.setDataAndType(videoUri, "video/*");
            playIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            getActivity().startActivity(playIntent);
        }
    }


    private boolean isValidPositionAndDimension() {


        Bitmap bitmap = BitmapFactory.decodeFile(imageFilePath);
        float boxWidth = xx2 - xx1;
        float boxHeight = yy2 - yy1;
        float cx2 = (float) boxWidth / (float) bitmap.getWidth();
        float cy2 = (float) boxHeight / (float) bitmap.getHeight();



        if ((xx1 == 0) && (xx2 == 0) && (yy1 == 0) && (yy2 == 0)) {
            dimensions = new PointF(0.5f, 0.5f);
            Log.e(TAG, "startVideoOverlayTransformation: sboxwidth = " + boxWidth + "boxHeight = " + boxHeight);
            Log.e(TAG, "isValidPositionAndDimension: xx1 = "+ xx1 + "xx2 = "+xx2 + "yy1 = "+yy1 + " yy2 = "+ yy2);
        } else {
            Log.e(TAG, "startVideoOverlayTransformation: boxwidth = " + boxWidth + "boxHeight = " + boxHeight);
            Log.e(TAG, "startVideoOverlayTransformation: cx2 " + cx2 + "cy2 = " + cy2);
            Log.e(TAG, "startVideoOverlayTransformation: width = " + bitmap.getWidth() + "height = " + bitmap.getHeight());
            dimensions = new PointF(cx2, cy2);
        }


        if ((xx1 == 0) && (xx2 == 0) && (yy1 == 0) && (yy2 == 0)) {
            position = new PointF(0.5f, 0.5f);
        } else {

            float cx1 = (float) xx1 / (float) bitmap.getWidth();
            float cy1 = (float) yy1 / (float) bitmap.getHeight();
            Log.e(TAG, "startVideoOverlayTransformation: xx1 = " + xx1 + "yy1 = " + yy1);
            Log.e(TAG, "startVideoOverlayTransformation: width = " + bitmap.getWidth() + "height = " + bitmap.getHeight());
            Log.e(TAG, "startVideoOverlayTransformation: cx1 " + cx1 + "cy1 = " + cy1);
            Log.e(TAG, "startVideoOverlayTransformation: mcx1 " + cx1 + (cx2 / 2) + "mcy1 = " + cy1 + (cy2 / 2));
            position = new PointF(cx1 + (cx2 / 2), cy1 + (cy2 / 2));
        }
        return true;
    }

    private void setPositionsAndDimensions() {
    }

    private void initAreaSelection() {

        final EditableImage image = new EditableImage(getArguments().getString("image"));

        ScalableBox box1 = new ScalableBox(25, 180, 640, 880);
        ScalableBox box2 = new ScalableBox(2, 18, 680, 880);
        ScalableBox box3 = new ScalableBox(250, 80, 400, 880);
        List<ScalableBox> boxes = new ArrayList<>();
        boxes.add(box1);
        boxes.add(box2);
        boxes.add(box3);
        image.setBoxes(boxes);
        binding.imageView.initView(getActivity(), image);

        binding.imageView.setOnBoxChangedListener(new OnBoxChangedListener() {
            @Override
            public void onChanged(int x1, int y1, int x2, int y2) {
                binding.tv.setText("box: [" + x1 + "," + y1 + "],[" + x2 + "," + y2 + "]");
                xx1 = x1;
                yy1 = y1;
                xx2 = x2;
                yy2 = y2;
            }
        });
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


    }


    public void startVideoOverlayTransformation(@NonNull SourceMedia sourceMedia, @NonNull TargetMedia targetMedia, @NonNull TargetVideoConfiguration targetVideoConfiguration, @NonNull TransformationState transformationState) {
        if (targetMedia.getIncludedTrackCount() < 1) {
            return;
        }

        if (targetMedia.targetFile.exists()) {
            targetMedia.targetFile.delete();
        }

        transformationState.requestId = UUID.randomUUID().toString();
        MediaTransformationListener transformationListener = new MediaTransformationListener(getActivity(), transformationState.requestId, transformationState, new OnProgressListener() {
            @Override
            public void onProgress(int progress) {
                binding.btnTranscode.setVisibility(View.GONE);
                binding.ly4.setVisibility(View.VISIBLE);
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.progressBar.setProgress(progress);
                Log.e("TAG", "setProgress: Progress = "+ progress );

            }

            @Override
            public void onCompleted() {
                binding.ly1.setVisibility(View.GONE);
                binding.progressBar.setVisibility(View.GONE);
                binding.btnPlay.setVisibility(View.VISIBLE);
                binding.tvInfo.setVisibility(View.VISIBLE);
                binding.tvInfo.setText("Your Video file has been successfully saved to "+targetMedia.targetFile);
                showInterstitialAds();

            }
        });

        try {
            MediaTarget mediaTarget = new MediaMuxerMediaTarget(targetMedia.targetFile.getPath(), targetMedia.getIncludedTrackCount(), targetVideoConfiguration.rotation, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

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
                TrackTransform.Builder trackTransformBuilder = new TrackTransform.Builder(mediaSource, targetTrack.sourceTrackIndex, mediaTarget).setTargetTrack(trackTransforms.size()).setTargetFormat(mediaFormat).setEncoder(new MediaCodecEncoder()).setDecoder(new MediaCodecDecoder());
                if (targetTrack.format instanceof VideoTrackFormat) {
                    // adding background bitmap first, to ensure that video renders on top of it
                    List<GlFilter> filters = new ArrayList<>();
                    if (targetMedia.backgroundImageUri != null) {
                        GlFilter backgroundImageFilter = TransformationUtil.createGlFilter(getActivity(), targetMedia.backgroundImageUri, new PointF(1, 1), new PointF(0.5f, 0.5f), 0);
                        filters.add(backgroundImageFilter);
                    }


                    Transform transform = new Transform(dimensions, position, 0);
                    //     Transform transform = new Transform(new PointF(1f, 1f), new PointF(1f, 0.55f), 0);
                    GlFrameRenderFilter frameRenderFilter = new DefaultVideoFrameRenderFilter(transform);
                    filters.add(frameRenderFilter);

                    trackTransformBuilder.setRenderer(new GlVideoRenderer(filters));
                }

                trackTransforms.add(trackTransformBuilder.build());
            }

            mediaTransformer.transform(transformationState.requestId, trackTransforms, transformationListener, MediaTransformer.GRANULARITY_DEFAULT);
        } catch (MediaTransformationException ex) {
            Log.e(TAG, "Exception when trying to perform track operation", ex);
        }


    }

    private void showInterstitialAds() {
        if (mInterstitialAd != null) {
            mInterstitialAd.show(getActivity());
        } else {
            Log.d("TAG", "The interstitial ad wasn't ready yet.");
        }
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


    public interface OnProgressListener {
        void onProgress(int progress);
        void onCompleted();
    }


}

