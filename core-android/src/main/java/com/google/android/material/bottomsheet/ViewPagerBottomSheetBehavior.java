package com.google.android.material.bottomsheet;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import androidx.viewpager.widget.ViewPager;

import java.lang.ref.WeakReference;

/**
 * Override {@link #findScrollingChild(View)} to support {@link ViewPager}'s nested scrolling.
 * <p>
 * By the way, In order to override package level method and field.
 * This class put in the same package path where {@link BottomSheetBehavior} located.
 */
public class ViewPagerBottomSheetBehavior<V extends View> extends BottomSheetBehavior<V> {
    public ViewPagerBottomSheetBehavior() {
        super();
    }

    public ViewPagerBottomSheetBehavior(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    View findScrollingChild(View view) {
        if (ViewCompat.isNestedScrollingEnabled(view)) return view;

        if (view instanceof ViewPager) {
            ViewPager viewPager = (ViewPager) view;
            View currentViewPagerChild = viewPager.getChildAt(viewPager.getCurrentItem());
            return findScrollingChild(currentViewPagerChild);
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0, count = group.getChildCount(); i < count; i++) {
                View scrollingChild = findScrollingChild(group.getChildAt(i));
                if (scrollingChild != null) {
                    return scrollingChild;
                }
            }
        }

        return null;
    }

    public void updateScrollingChild() {
        final View view = viewRef != null ? viewRef.get() : null;
        if (view == null) return;
        final View scrollingChild = findScrollingChild(view);
        nestedScrollingChildRef = new WeakReference<>(scrollingChild);
    }

    /**
     * A utility function to get the {@link ViewPagerBottomSheetBehavior} associated with the {@code view}.
     *
     * @param view The {@link View} with {@link ViewPagerBottomSheetBehavior}.
     * @return The {@link ViewPagerBottomSheetBehavior} associated with the {@code view}.
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public static <V extends View> ViewPagerBottomSheetBehavior<V> from(V view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (!(params instanceof CoordinatorLayout.LayoutParams)) {
            throw new IllegalArgumentException("The view is not a child of CoordinatorLayout");
        }
        CoordinatorLayout.Behavior<?> behavior = ((CoordinatorLayout.LayoutParams) params).getBehavior();
        if (!(behavior instanceof ViewPagerBottomSheetBehavior)) {
            throw new IllegalArgumentException(
                    "The view is not associated with ViewPagerBottomSheetBehavior");
        }
        return (ViewPagerBottomSheetBehavior<V>) behavior;
    }
}