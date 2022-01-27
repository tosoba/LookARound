package com.lookaround.core.android.map.clustering;

import android.content.Context;
import android.graphics.RectF;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lookaround.core.android.map.ext.BoundingBox;
import com.lookaround.core.android.map.ext.LatLon;
import com.lookaround.core.android.map.ext.MapControllerExtKt;
import com.mapzen.tangram.MapChangeListener;
import com.mapzen.tangram.MapController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ClusterManager<T extends ClusterItem> implements MapChangeListener {

    private static final int QUAD_TREE_BUCKET_CAPACITY = 4;
    private static final int DEFAULT_MIN_CLUSTER_SIZE = 1;

    private final MapController mapController;

    private final QuadTree<T> mQuadTree;

    private final ClusterRenderer<T> mRenderer;

    private final Executor mExecutor = Executors.newSingleThreadExecutor();

    private AsyncTask mQuadTreeTask;

    private AsyncTask mClusterTask;

    private int mMinClusterSize = DEFAULT_MIN_CLUSTER_SIZE;

    @Override
    public void onViewComplete() {

    }

    @Override
    public void onRegionWillChange(boolean b) {

    }

    @Override
    public void onRegionIsChanging() {

    }

    @Override
    public void onRegionDidChange(boolean b) {
        cluster();
    }

    /**
     * Defines signatures for methods that are called when a cluster or a cluster item is clicked.
     *
     * @param <T> the type of an item managed by {@link ClusterManager}.
     */
    public interface Callbacks<T extends ClusterItem> {
        /**
         * Called when a marker representing a cluster has been clicked.
         *
         * @param cluster the cluster that has been clicked
         * @return <code>true</code> if the listener has consumed the event (i.e., the default behavior should not occur);
         * <code>false</code> otherwise (i.e., the default behavior should occur). The default behavior is for the camera
         * to move to the marker and an info window to appear.
         */
        void onClusterClick(@NonNull Cluster<T> cluster);

        /**
         * Called when a marker representing a cluster item has been clicked.
         *
         * @param clusterItem the cluster item that has been clicked
         * @return <code>true</code> if the listener has consumed the event (i.e., the default behavior should not occur);
         * <code>false</code> otherwise (i.e., the default behavior should occur). The default behavior is for the camera
         * to move to the marker and an info window to appear.
         */
        void onClusterItemClick(@NonNull T clusterItem);
    }

    /**
     * Creates a new cluster manager using the default icon generator.
     * To customize marker icons, set a custom icon generator using
     * {@link ClusterManager#setIconGenerator(IconGenerator)}.
     *
     * @param mapController the map instance where markers will be rendered
     */
    public ClusterManager(@NonNull Context context, @NonNull MapController mapController) {
        Preconditions.checkNotNull(context);
        this.mapController = Preconditions.checkNotNull(mapController);
        mRenderer = new ClusterRenderer<>(context, mapController);
        mQuadTree = new QuadTree<>(QUAD_TREE_BUCKET_CAPACITY);
    }

    /**
     * Sets a custom icon generator thus replacing the default one.
     *
     * @param iconGenerator the custom icon generator that's used for generating marker icons
     */
    public void setIconGenerator(@NonNull IconGenerator<T> iconGenerator) {
        Preconditions.checkNotNull(iconGenerator);
        mRenderer.setIconGenerator(iconGenerator);
    }

    /**
     * Sets a callback that's invoked when a cluster or a cluster item is clicked.
     *
     * @param callbacks the callback that's invoked when a cluster or an individual item is clicked.
     *                  To unset the callback, use <code>null</code>.
     */
    public void setCallbacks(@Nullable Callbacks<T> callbacks) {
        mRenderer.setCallbacks(callbacks);
    }

    /**
     * Sets items to be clustered thus replacing the old ones.
     *
     * @param clusterItems the items to be clustered
     */
    public void setItems(@NonNull List<T> clusterItems) {
        Preconditions.checkNotNull(clusterItems);
        buildQuadTree(clusterItems);
    }

    /**
     * Sets the minimum size of a cluster. If the cluster size
     * is less than this value, display individual markers.
     */
    public void setMinClusterSize(int minClusterSize) {
        Preconditions.checkArgument(minClusterSize > 0);
        mMinClusterSize = minClusterSize;
    }

    private void buildQuadTree(@NonNull List<T> clusterItems) {
        if (mQuadTreeTask != null) {
            mQuadTreeTask.cancel(true);
        }

        mQuadTreeTask = new QuadTreeTask(clusterItems).executeOnExecutor(mExecutor);
    }

    private void cluster() {
        if (mClusterTask != null) {
            mClusterTask.cancel(true);
        }

        float zoom = mapController.getCameraPosition().getZoom();
        BoundingBox screenBoundingBox = MapControllerExtKt.screenAreaToBoundingBox(mapController, new RectF());
        mClusterTask = new ClusterTask(screenBoundingBox.component2(), screenBoundingBox.component1(),
                zoom).executeOnExecutor(mExecutor);
    }

    @NonNull
    private List<Cluster<T>> getClusters(@NonNull LatLon northEast, @NonNull LatLon southWest, float zoomLevel) {
        List<Cluster<T>> clusters = new ArrayList<>();

        long tileCount = (long) (Math.pow(2, zoomLevel) * 2);

        double startLatitude = northEast.getLatitude();
        double endLatitude = southWest.getLatitude();

        double startLongitude = southWest.getLongitude();
        double endLongitude = northEast.getLongitude();

        double stepLatitude = 180.0 / tileCount;
        double stepLongitude = 360.0 / tileCount;

        if (startLongitude > endLongitude) { // Longitude +180°/-180° overlap.
            // [start longitude; 180]
            getClustersInsideBounds(clusters, startLatitude, endLatitude,
                    startLongitude, 180.0, stepLatitude, stepLongitude);
            // [-180; end longitude]
            getClustersInsideBounds(clusters, startLatitude, endLatitude,
                    -180.0, endLongitude, stepLatitude, stepLongitude);
        } else {
            getClustersInsideBounds(clusters, startLatitude, endLatitude,
                    startLongitude, endLongitude, stepLatitude, stepLongitude);
        }

        return clusters;
    }

    private void getClustersInsideBounds(@NonNull List<Cluster<T>> clusters,
                                         double startLatitude, double endLatitude,
                                         double startLongitude, double endLongitude,
                                         double stepLatitude, double stepLongitude) {
        long startX = (long) ((startLongitude + 180.0) / stepLongitude);
        long startY = (long) ((90.0 - startLatitude) / stepLatitude);

        long endX = (long) ((endLongitude + 180.0) / stepLongitude) + 1;
        long endY = (long) ((90.0 - endLatitude) / stepLatitude) + 1;

        for (long tileX = startX; tileX <= endX; tileX++) {
            for (long tileY = startY; tileY <= endY; tileY++) {
                double north = 90.0 - tileY * stepLatitude;
                double west = tileX * stepLongitude - 180.0;
                double south = north - stepLatitude;
                double east = west + stepLongitude;

                List<T> points = mQuadTree.queryRange(north, west, south, east);

                if (points.isEmpty()) {
                    continue;
                }

                if (points.size() >= mMinClusterSize) {
                    double totalLatitude = 0;
                    double totalLongitude = 0;

                    for (T point : points) {
                        totalLatitude += point.getLatitude();
                        totalLongitude += point.getLongitude();
                    }

                    double latitude = totalLatitude / points.size();
                    double longitude = totalLongitude / points.size();

                    clusters.add(new Cluster<>(latitude, longitude,
                            points, north, west, south, east));
                } else {
                    for (T point : points) {
                        clusters.add(new Cluster<>(point.getLatitude(), point.getLongitude(),
                                Collections.singletonList(point), north, west, south, east));
                    }
                }
            }
        }
    }

    private class QuadTreeTask extends AsyncTask<Void, Void, Void> {

        private final List<T> mClusterItems;

        private QuadTreeTask(@NonNull List<T> clusterItems) {
            mClusterItems = clusterItems;
        }

        @Override
        protected Void doInBackground(Void... params) {
            mQuadTree.clear();
            for (T clusterItem : mClusterItems) {
                mQuadTree.insert(clusterItem);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            cluster();
            mQuadTreeTask = null;
        }
    }

    private class ClusterTask extends AsyncTask<Void, Void, List<Cluster<T>>> {
        private final LatLon northEast;
        private final LatLon southWest;
        private final float mZoomLevel;

        private ClusterTask(@NonNull LatLon northEast, @NonNull LatLon southWest, float zoomLevel) {
            this.northEast = northEast;
            this.southWest = southWest;
            mZoomLevel = zoomLevel;
        }

        @Override
        protected List<Cluster<T>> doInBackground(Void... params) {
            return getClusters(northEast, southWest, mZoomLevel);
        }

        @Override
        protected void onPostExecute(@NonNull List<Cluster<T>> clusters) {
            mRenderer.render(clusters);
            mClusterTask = null;
        }
    }
}
