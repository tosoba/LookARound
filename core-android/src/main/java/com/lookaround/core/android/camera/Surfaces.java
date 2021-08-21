package com.lookaround.core.android.camera;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.view.Surface;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;

/**
 * Utilities for working with {@link Surface Surfaces}.
 */
final class Surfaces {

    public static final int ROTATION_0_DEG = 0;
    public static final int ROTATION_90_DEG = 90;
    public static final int ROTATION_180_DEG = 180;
    public static final int ROTATION_270_DEG = 270;

    @Retention(SOURCE)
    @IntDef({ROTATION_0_DEG, ROTATION_90_DEG, ROTATION_180_DEG, ROTATION_270_DEG})
    public @interface RotationDegrees {
    }

    @Retention(SOURCE)
    @IntDef({Surface.ROTATION_0, Surface.ROTATION_90, Surface.ROTATION_180, Surface.ROTATION_270})
    public @interface RotationEnum {
    }

    @RotationDegrees
    public static int toSurfaceRotationDegrees(@RotationEnum int rotationEnum) {
        @RotationDegrees int rotationDegrees;
        switch (rotationEnum) {
            case Surface.ROTATION_0:
                rotationDegrees = ROTATION_0_DEG;
                break;
            case Surface.ROTATION_90:
                rotationDegrees = ROTATION_90_DEG;
                break;
            case Surface.ROTATION_180:
                rotationDegrees = ROTATION_180_DEG;
                break;
            case Surface.ROTATION_270:
                rotationDegrees = ROTATION_270_DEG;
                break;
            default:
                throw new UnsupportedOperationException(
                        "Unsupported rotation enum: " + rotationEnum);
        }
        return rotationDegrees;
    }

    private Surfaces() {
    }
}
