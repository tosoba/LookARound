package com.lookaround.core.android.map.clustering;

import androidx.annotation.Nullable;

import com.lookaround.core.android.map.ext.LatLon;

public class DefaultClusterItem implements ClusterItem {
    private final LatLon latLon;

    public DefaultClusterItem(LatLon latLon) {
        this.latLon = latLon;
    }

    @Override
    public double getLatitude() {
        return latLon.getLatitude();
    }

    @Override
    public double getLongitude() {
        return latLon.getLongitude();
    }

    @Nullable
    @Override
    public String getTitle() {
        return null;
    }

    @Nullable
    @Override
    public String getSnippet() {
        return null;
    }
}
