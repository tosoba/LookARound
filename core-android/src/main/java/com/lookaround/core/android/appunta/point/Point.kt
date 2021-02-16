package com.lookaround.core.android.appunta.point;

import android.location.Location;

import com.lookaround.core.android.appunta.renderer.PointRenderer;

import java.util.UUID;

public interface Point {

    /***
     * Distance to a point
     * @return the distance in Km if previously set
     */
    double getDistance();

    /***
     * Allows to store the distance to a point
     * @param distance Distance to a point in Km
     */
    void setDistance(double distance);

    /***
     * Name of the point. Created in order to make your life easier
     * @return the name of the point
     */
    String getName();

    /***
     * Allows to store a name for the point
     * @param name the intended name
     */
    void setName(String name);

    /***
     * Gets the name of the renderer to use to draw this point
     * @return The renderer
     */
    PointRenderer getRenderer();

    /***
     * To assign a renderer to the current point
     * @param renderer
     */
    void setRenderer(PointRenderer renderer);

    /***
     * A unique id
     * @return an id
     */
    UUID getId();

    /***
     * A unique id
     * @param id
     */
    void setId(UUID id);

    /***
     * Last X coordinate where the point should be drawn
     * @return X coordinate of the canvas
     */
    float getX();

    void setX(float x);

    /***
     * Last Y coordinate where the point should be drawn
     * @return Y coordinate of the canvas
     */
    float getY();

    void setY(float y);

    Location getLocation();

    void setLocation(Location location);

    boolean isSelected();

    void setSelected(boolean selected);

    boolean isDrawn();

    void setDrawn(boolean drawn);

}