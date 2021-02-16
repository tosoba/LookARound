package com.lookaround.core.android.appunta.point;

import android.location.Location;

import com.lookaround.core.android.appunta.renderer.PointRenderer;

import java.util.UUID;

/***
 * A single point representing a place, it contains information on where it's
 * located in space, in screen, it's id and name and the name of the renderer to
 * use to draw it.
 */
public class SimplePoint implements Point {
    private UUID id;
    private Location location;
    private double distance;
    private String name;
    private PointRenderer renderer;
    private float x;
    private float y;
    private boolean selected;
    private boolean drawn = true;

    public SimplePoint(UUID id, Location location, PointRenderer renderer, String name) {
        this.id = id;
        this.setLocation(location);
        this.renderer = renderer;
        this.name = name;
    }

    public SimplePoint(UUID id, Location location, PointRenderer renderer) {
        this(id, location, renderer, "");
    }

    public SimplePoint(UUID id, Location location) {
        this(id, location, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimplePoint that = (SimplePoint) o;
        return that.id.equals(id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /* (non-Javadoc)
     * @see com.appunta.android.point.Point#getDistance()
     */
    @Override
    public double getDistance() {
        return distance;
    }

    /* (non-Javadoc)
     * @see com.appunta.android.point.Point#setDistance(double)
     */
    @Override
    public void setDistance(double distance) {
        this.distance = distance;
    }

    /* (non-Javadoc)
     * @see com.appunta.android.point.Point#getName()
     */
    @Override
    public String getName() {
        return name;
    }

    /* (non-Javadoc)
     * @see com.appunta.android.point.Point#setName(java.lang.String)
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }

    /* (non-Javadoc)
     * @see com.appunta.android.point.Point#getRenderer()
     */
    @Override
    public PointRenderer getRenderer() {
        return renderer;
    }

    /* (non-Javadoc)
     * @see com.appunta.android.point.Point#setRenderer(com.appunta.android.point.renderer.PointRenderer)
     */
    @Override
    public void setRenderer(PointRenderer renderer) {
        this.renderer = renderer;
    }

    /* (non-Javadoc)
     * @see com.appunta.android.point.Point#getId()
     */
    @Override
    public UUID getId() {
        return id;
    }

    /* (non-Javadoc)
     * @see com.appunta.android.point.Point#setId(int)
     */
    @Override
    public void setId(UUID id) {
        this.id = id;
    }

    /* (non-Javadoc)
     * @see com.appunta.android.point.Point#getX()
     */
    @Override
    public float getX() {
        return x;
    }

    /* (non-Javadoc)
     * @see com.appunta.android.point.Point#setX(float)
     */
    @Override
    public void setX(float x) {
        this.x = x;
    }

    /* (non-Javadoc)
     * @see com.appunta.android.point.Point#getY()
     */
    @Override
    public float getY() {
        return y;
    }

    /* (non-Javadoc)
     * @see com.appunta.android.point.Point#setY(float)
     */
    @Override
    public void setY(float y) {
        this.y = y;
    }

    /* (non-Javadoc)
     * @see com.appunta.android.point.Point#getLocation()
     */
    @Override
    public Location getLocation() {
        return location;
    }

    /* (non-Javadoc)
     * @see com.appunta.android.point.Point#setLocation(android.location.Location)
     */
    @Override
    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public boolean isDrawn() {
        return drawn;
    }

    @Override
    public void setDrawn(boolean drawn) {
        this.drawn = drawn;
    }
}
