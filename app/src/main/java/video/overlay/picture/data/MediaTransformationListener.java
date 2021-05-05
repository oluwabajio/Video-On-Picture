/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package video.overlay.picture.data;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.linkedin.android.litr.TransformationListener;
import com.linkedin.android.litr.analytics.TrackTransformationInfo;

import java.util.List;

import video.overlay.picture.PlayerFragment;
import video.overlay.picture.data.utils.TrackMetadataUtil;

public class MediaTransformationListener implements TransformationListener {

    private final Context context;
    private final String requestId;
    private final TransformationState transformationState;
    private PlayerFragment.OnProgressListener onProgressListener;

    public MediaTransformationListener(@NonNull Context context, @NonNull String requestId, @NonNull TransformationState transformationState, PlayerFragment.OnProgressListener onProgressListener) {
        this.context = context;
        this.requestId = requestId;
        this.transformationState = transformationState;
        this.onProgressListener = onProgressListener;
    }

    @Override
    public void onStarted(@NonNull String id) {
        if (TextUtils.equals(requestId, id)) {
            transformationState.setState(TransformationState.STATE_RUNNING);
        }
    }

    @Override
    public void onProgress(@NonNull String id, float progress) {
        if (TextUtils.equals(requestId, id)) {
            transformationState.setProgress((int) (progress * TransformationState.MAX_PROGRESS));
            onProgressListener.onProgress((int) Math.round(progress));
        }
    }

    @Override
    public void onCompleted(@NonNull String id, @Nullable List<TrackTransformationInfo> trackTransformationInfos) {
        if (TextUtils.equals(requestId, id)) {
            transformationState.setState(TransformationState.STATE_COMPLETED);
            transformationState.setProgress(TransformationState.MAX_PROGRESS);
            transformationState.setStats(TrackMetadataUtil.printTransformationStats(context, trackTransformationInfos));
            onProgressListener.onCompleted();
        }
    }

    @Override
    public void onCancelled(@NonNull String id, @Nullable List<TrackTransformationInfo> trackTransformationInfos) {
        if (TextUtils.equals(requestId, id)) {
            transformationState.setState(TransformationState.STATE_CANCELLED);
            transformationState.setStats(TrackMetadataUtil.printTransformationStats(context, trackTransformationInfos));
        }
    }

    @Override
    public void onError(@NonNull String id, @Nullable Throwable cause, @Nullable List<TrackTransformationInfo> trackTransformationInfos) {
        if (TextUtils.equals(requestId, id)) {
            transformationState.setState(TransformationState.STATE_ERROR);
            transformationState.setStats(TrackMetadataUtil.printTransformationStats(context, trackTransformationInfos));
        }
    }

}
