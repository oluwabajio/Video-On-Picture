package video.overlay.picture.utils;

import video.overlay.picture.TargetMedia;
import video.overlay.picture.data.SourceMedia;
import video.overlay.picture.data.TargetVideoConfiguration;
import video.overlay.picture.data.TransformationState;

public class MediaUtils {

    private TargetMedia targetMedia;
    private SourceMedia sourceMedia;
    TargetVideoConfiguration targetVideoConfiguration;
    private TransformationState transformationState;

    private static MediaUtils mediaUtils;

    private MediaUtils() {
    }

    public static MediaUtils getInstance() {
        if (mediaUtils == null) {
            mediaUtils = new MediaUtils();
        }
        return mediaUtils;
    }


    public SourceMedia getSourceMedia() {
        return sourceMedia;
    }

    public void setSourceMedia(SourceMedia sourceMedia) {
        this.sourceMedia = sourceMedia;
    }

    public static MediaUtils getMediaUtils() {
        return mediaUtils;
    }

    public static void setMediaUtils(MediaUtils mediaUtils) {
        MediaUtils.mediaUtils = mediaUtils;
    }

    public TargetMedia getTargetMedia() {
        return targetMedia;
    }

    public void setTargetMedia(TargetMedia targetMedia) {
        this.targetMedia = targetMedia;
    }


    public TargetVideoConfiguration getTargetVideoConfiguration() {
        return targetVideoConfiguration;
    }

    public void setTargetVideoConfiguration(TargetVideoConfiguration targetVideoConfiguration) {
        this.targetVideoConfiguration = targetVideoConfiguration;
    }

    public TransformationState getTransformationState() {
        return transformationState;
    }

    public void setTransformationState(TransformationState transformationState) {
        this.transformationState = transformationState;
    }
}
