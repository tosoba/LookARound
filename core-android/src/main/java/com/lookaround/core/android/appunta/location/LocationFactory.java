package com.lookaround.core.android.appunta.location;

import android.location.Location;

/**
 * A helper intended to build a new location from simple values
 *
 * @author Sergi Mart?nez
 */
public class LocationFactory {
    /***
     * Creates a new location with the data provided
     *
     * @param latitude
     *            Latitude of the point
     * @param longitude
     *            Longitude of the point
     * @param altitude
     *            Altitude in Km of the point
     * @return A location object with the data provided
     */
    public static Location createLocation(double latitude, double longitude, double altitude) {
        Location location = new Location("");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setAltitude(altitude);
        location.setAccuracy(0);
        return location;
    }

    /***
     * Creates a new location with the data provided
     *
     * @param latitude
     *            Latitude of the point
     * @param longitude
     *            Longitude of the point
     * @return A location object with the data provided
     */
    public static Location createLocation(double latitude, double longitude) {
        return createLocation(latitude, longitude, 0);
    }
}
