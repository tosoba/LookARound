package com.lookaround.core.android.map.clustering;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.lookaround.core.android.map.ext.LatLon;
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

    private static final int BACKGROUND_MARKER_Z_INDEX = 0;

    private static final int FOREGROUND_MARKER_Z_INDEX = 1;

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

        // Remove the old clusters.
        for (Cluster<T> clusterToRemove : clustersToRemove) {
            Marker markerToRemove = mMarkers.get(clusterToRemove);
//            markerToRemove.setZIndex(BACKGROUND_MARKER_Z_INDEX);

            Cluster<T> parentCluster = findParentCluster(mClusters, clusterToRemove.getLatitude(),
                    clusterToRemove.getLongitude());
            if (parentCluster != null) {
                //TODO: replace this - most likely crash cause Marker has no position property
                animateMarkerToLocation(markerToRemove, new LatLon(parentCluster.getLatitude(),
                        parentCluster.getLongitude()), true);
            } else {
                mapController.removeMarker(markerToRemove);
            }

            mMarkers.remove(clusterToRemove);
        }

        // Add the new clusters.
        for (Cluster<T> clusterToAdd : clustersToAdd) {
            Marker markerToAdd;

            Bitmap markerIcon = getMarkerIcon(clusterToAdd);
            String markerTitle = getMarkerTitle(clusterToAdd);
            String markerSnippet = getMarkerSnippet(clusterToAdd);

            Cluster<T> parentCluster = findParentCluster(clustersToRemove, clusterToAdd.getLatitude(),
                    clusterToAdd.getLongitude());
            markerToAdd = mapController.addMarker();
            if (parentCluster != null) {
                markerToAdd.setPoint(new LngLat(parentCluster.getLongitude(), parentCluster.getLatitude()));
                markerToAdd.setDrawable(new BitmapDrawable(context.getResources(), markerIcon));
                animateMarkerToLocation(markerToAdd,
                        new LatLon(clusterToAdd.getLatitude(), clusterToAdd.getLongitude()), false);
            } else {
                markerToAdd.setPoint(new LngLat(clusterToAdd.getLatitude(), clusterToAdd.getLongitude()));
                markerToAdd.setDrawable(new BitmapDrawable(context.getResources(), markerIcon));
                animateMarkerAppearance(markerToAdd);
            }
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

    @Nullable
    private String getMarkerTitle(@NonNull Cluster<T> cluster) {
        List<T> clusterItems = cluster.getItems();
        if (clusterItems.size() > 1) {
            return null;
        } else {
            return clusterItems.get(0).getTitle();
        }
    }

    @Nullable
    private String getMarkerSnippet(@NonNull Cluster<T> cluster) {
        List<T> clusterItems = cluster.getItems();
        if (clusterItems.size() > 1) {
            return null;
        } else {
            return clusterItems.get(0).getSnippet();
        }
    }

    @Nullable
    private Cluster<T> findParentCluster(@NonNull List<Cluster<T>> clusters,
                                         double latitude, double longitude) {
        for (Cluster<T> cluster : clusters) {
            if (cluster.contains(latitude, longitude)) {
                return cluster;
            }
        }

        return null;
    }

    private void animateMarkerToLocation(@NonNull final Marker marker, @NonNull LatLon targetLocation,
                                         final boolean removeAfter) {
        ObjectAnimator objectAnimator = ObjectAnimator.ofObject(marker, "position",
                new LatLonTypeEvaluator(), targetLocation);
        objectAnimator.setInterpolator(new FastOutSlowInInterpolator());
        objectAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (removeAfter) {
                    mapController.removeMarker(marker);
                }
            }
        });
        objectAnimator.start();
    }

    private void animateMarkerAppearance(@NonNull Marker marker) {
//     TODO:   ObjectAnimator.ofFloat(marker, "alpha", 1.0F).start();
    }

    @Override
    public void onMarkerPickComplete(@Nullable MarkerPickResult markerPickResult) {
        if (markerPickResult == null || markerPickResult.getMarker() == null) return;

        Object markerTag = markerPickResult.getMarker().getUserData();
        if (markerTag instanceof Cluster) {
            //noinspection unchecked
            Cluster<T> cluster = (Cluster<T>) markerPickResult.getMarker().getUserData();
            //noinspection ConstantConditions
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

    private static class LatLonTypeEvaluator implements TypeEvaluator<LatLon> {

        @Override
        public LatLon evaluate(float fraction, LatLon startValue, LatLon endValue) {
            double latitude = (endValue.getLatitude() - startValue.getLatitude()) * fraction + startValue.getLatitude();
            double longitude = (endValue.getLongitude() - startValue.getLongitude()) * fraction + startValue.getLongitude();
            return new LatLon(latitude, longitude);
        }
    }
}
