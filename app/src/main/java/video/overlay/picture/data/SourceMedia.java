/*
 * Copyright 2019 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package video.overlay.picture.data;

import android.net.Uri;



import java.util.ArrayList;
import java.util.List;

public class SourceMedia  {

    public Uri uri;
    public long size;
    public float duration;

    public List<MediaTrackFormat> tracks = new ArrayList<>();
}
