/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package video.overlay.picture.data;

import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;


import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class TransformationState implements Serializable {

    public static final int MAX_PROGRESS = 100;

    public static final int STATE_IDLE = 0;
    public static final int STATE_RUNNING = 1;
    public static final int STATE_COMPLETED = 3;
    public static final int STATE_CANCELLED = 4;
    public static final int STATE_ERROR = 5;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ STATE_IDLE, STATE_RUNNING, STATE_COMPLETED, STATE_CANCELLED, STATE_ERROR})
    @interface State {}

    public String requestId;

    public int state;
    public int progress;
    public String stats;

    public TransformationState() {
        state = STATE_IDLE;
        progress = 0;
        stats = null;
    }

    public void setState(@State int state) {
        this.state = state;
        
    }

    public void setProgress(int progress) {
        this.progress = progress;
//        Log.e("TAG", "setProgress: Progress = "+ progress );
        
    }

    public void setStats(@Nullable String stats) {
        this.stats = stats;
        
    }
}
