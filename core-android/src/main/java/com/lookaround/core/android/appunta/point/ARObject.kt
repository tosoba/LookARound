package com.lookaround.core.android.appunta.point;

import android.location.Location;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ARObject {
    private static final Map<UUID, ARObject> objects = new HashMap<>();
    private final Point point;
    private Float screenY = null;

    public ARObject(Point point) {
        this.point = point;
        if (!objects.containsKey(point.getId())) objects.put(point.getId(), this);
    }

    public static Collection<ARObject> getObjects() {
        return objects.values();
    }

    public static ARObject findByPoint(Point point) {
        return objects.get(point.getId());
    }

    public Float getScreenY() {
        return screenY;
    }

    public void setScreenY(float screenY) {
        this.screenY = screenY;
    }

    public Point getPoint() {
        return point;
    }

    public Location getLocation() {
        return point.getLocation();
    }

    public boolean willOverlapWith(ARObject other, float dialogWidth) {
        if (this.point.equals(other.point)) return false;
        return Math.abs(this.point.getX() - other.point.getX()) < dialogWidth * 1.2f;
    }
}
