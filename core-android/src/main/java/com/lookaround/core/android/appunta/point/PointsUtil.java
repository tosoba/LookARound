package com.lookaround.core.android.appunta.point;

import android.location.Location;

import java.util.ArrayList;
import java.util.List;

/***
 * A simple utility class intented to  perform basic operations
 */
public class PointsUtil {
    private static final int EARTH_RADIUS_METERS = 6371000;

    /***
     * Calculate the distance from a given point to all the points stored and
     * sets the distance property for all them
     *
     * @param location
     *            Latitude and longitude of the given point
     */
    public static void calculateDistance(List<? extends Point> points, Location location) {
        for (Point poi : points) {
            poi.setDistance(distanceInMeters(poi.getLocation(), location));
        }
    }

    /***
     * Returns a subset of points that are below a distance of a given point
     *
     * @param location
     *            Latitude and longitude of the given point
     * @param distance
     *            Distance to filter
     * @return The subset list
     */
    public static List<Point> getNearPoints(List<Point> points, Location location, double distance) {
        calculateDistance(points, location);
        List<Point> subPoints = new ArrayList<>();
        for (Point poi : points) {
            if (poi.getDistance() <= distance) {
                subPoints.add(poi);
            }
        }
        return subPoints;
    }

    /**
     * Computes the distance in meters between two points on Earth.
     *
     * @param location      Latitude and longitude of the first point
     * @param otherLocation Latitude and longitude of the second point
     * @return Distance between the two points in kilometers.
     */
    private static double distanceInMeters(Location location, Location otherLocation) {
        double lat1Rad = Math.toRadians(otherLocation.getLatitude());
        double lat2Rad = Math.toRadians(location.getLatitude());
        double deltaLonRad = Math.toRadians(location.getLongitude()
                - otherLocation.getLongitude());

        return Math
                .acos(Math.sin(lat1Rad) * Math.sin(lat2Rad) + Math.cos(lat1Rad)
                        * Math.cos(lat2Rad) * Math.cos(deltaLonRad))
                * EARTH_RADIUS_METERS;
    }

    public static double calculateBearing(Location pos1, Location pos2) {
        double lat1 = Math.toRadians(pos1.getLatitude());
        double lat2 = Math.toRadians(pos2.getLatitude());
        double lng1 = Math.toRadians(pos1.getLongitude());
        double lng2 = Math.toRadians(pos2.getLongitude());
        return calculateBearing(lat1, lng1, lat2, lng2);
    }

    private static double calculateBearing(double lat1, double long1, double lat2, double long2) {
        double dLon = long2 - long1;

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);

        double brng = Math.atan2(y, x);

        brng = Math.toDegrees(brng);
        brng = (brng + 360) % 360;
        // brng = 360 - brng // count degrees counter-clockwise - remove to make clockwise
        if (brng > 180) brng = 360 - brng;
        return brng;
    }
}
