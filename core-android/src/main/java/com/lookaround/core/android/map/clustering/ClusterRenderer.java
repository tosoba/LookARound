package com.lookaround.core.android.map.clustering;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mapzen.tangram.LngLat;
import com.mapzen.tangram.MapController;
import com.mapzen.tangram.Marker;
import com.mapzen.tangram.MarkerPickListener;
import com.mapzen.tangram.MarkerPickResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ClusterRenderer<T extends ClusterItem> implements MarkerPickListener {
    private final MapController mapController;

    private final List<Cluster<T>> mClusters = new ArrayList<>();

    private final Map<Cluster<T>, Marker> mMarkers = new HashMap<>();

    private IconGenerator<T> mIconGenerator;

    private ClusterManager.Callbacks<T> mCallbacks;

    private final Context context;

    ClusterRenderer(@NonNull Context context, @NonNull MapController mapController) {
        this.mapController = mapController;
        this.mapController.setMarkerPickListener(this);
        this.context = context;
        mIconGenerator = new DefaultIconGenerator<>(context);
    }

    void setCallbacks(@Nullable ClusterManager.Callbacks<T> listener) {
        mCallbacks = listener;
    }

    void setIconGenerator(@NonNull IconGenerator<T> iconGenerator) {
        mIconGenerator = iconGenerator;
    }

    void render(@NonNull List<Cluster<T>> clusters) {
        List<Cluster<T>> clustersToAdd = new ArrayList<>();
        List<Cluster<T>> clustersToRemove = new ArrayList<>();

        for (Cluster<T> cluster : clusters) {
            if (!mMarkers.containsKey(cluster)) {
                clustersToAdd.add(cluster);
            }
        }

        for (Cluster<T> cluster : mMarkers.keySet()) {
            if (!clusters.contains(cluster)) {
                clustersToRemove.add(cluster);
            }
        }

        mClusters.addAll(clustersToAdd);
        mClusters.removeAll(clustersToRemove);

        for (Cluster<T> clusterToRemove : clustersToRemove) {
            Marker markerToRemove = mMarkers.get(clusterToRemove);
            if (markerToRemove == null) continue;
            mapController.removeMarker(markerToRemove);

            mMarkers.remove(clusterToRemove);
        }

        for (Cluster<T> clusterToAdd : clustersToAdd) {
            Marker markerToAdd = mapController.addMarker();
            markerToAdd.setPoint(new LngLat(clusterToAdd.getLongitude(), clusterToAdd.getLatitude()));
            markerToAdd.setStylingFromString(
                    "{ style: 'points', size: [27px, 27px], order: 2000, collide: false, color: blue}"
            );
            Bitmap markerIcon = getMarkerIcon(clusterToAdd);
            markerToAdd.setDrawable(new BitmapDrawable(context.getResources(), markerIcon));
            markerToAdd.setVisible(true);
            markerToAdd.setUserData(clusterToAdd);

            mMarkers.put(clusterToAdd, markerToAdd);
        }
    }

    @NonNull
    private Bitmap getMarkerIcon(@NonNull Cluster<T> cluster) {
        Bitmap clusterIcon;

        List<T> clusterItems = cluster.getItems();
        if (clusterItems.size() > 1) {
            clusterIcon = mIconGenerator.getClusterIcon(cluster);
        } else {
            clusterIcon = mIconGenerator.getClusterItemIcon(clusterItems.get(0));
        }

        return Preconditions.checkNotNull(clusterIcon);
    }

    @Override
    public void onMarkerPickComplete(@Nullable MarkerPickResult markerPickResult) {
        if (markerPickResult == null || markerPickResult.getMarker() == null) return;

        Object markerTag = markerPickResult.getMarker().getUserData();
        if (markerTag instanceof Cluster) {
            //noinspection unchecked
            Cluster<T> cluster = (Cluster<T>) markerPickResult.getMarker().getUserData();
            List<T> clusterItems = cluster.getItems();
            if (mCallbacks != null) {
                if (clusterItems.size() > 1) {
                    mCallbacks.onClusterClick(cluster);
                } else {
                    mCallbacks.onClusterItemClick(clusterItems.get(0));
                }
            }
        }
    }
}
