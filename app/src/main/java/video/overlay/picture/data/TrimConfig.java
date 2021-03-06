/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package video.overlay.picture.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import com.google.android.material.slider.RangeSlider;

import java.util.ArrayList;
import java.util.List;

public class TrimConfig {

    public final RangeSlider.OnChangeListener onValueChangeListener = (slider, value, fromUser) -> {
        range = slider.getValues();
    };

    public boolean enabled;
    public List<Float> range = new ArrayList<>(2);

    public TrimConfig() {
        range.add(0f);
        range.add(1f);
    }


    public Boolean getEnabled() {
        return enabled;
    }


    public static void setOnChangeListener(@NonNull RangeSlider rangeSlider, @Nullable RangeSlider.OnChangeListener onChangeListener) {
        rangeSlider.addOnChangeListener(onChangeListener);
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;

    }

    public void setTrimEnd(float trimEnd) {
        range.set(1, trimEnd);

    }
}
