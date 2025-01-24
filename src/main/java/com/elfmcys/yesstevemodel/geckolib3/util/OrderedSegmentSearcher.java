package com.elfmcys.yesstevemodel.geckolib3.util;

import java.util.List;
import java.util.function.ToDoubleFunction;

public class OrderedSegmentSearcher<T> {
    private final List<T> segments;
    private final double leftBound;
    private final double rightBound;
    private final ToDoubleFunction<T> rightBoundGetter;

    private int currentIndex;
    private double currentLeft;
    private double currentRight;

    public OrderedSegmentSearcher(List<T> segments, double leftBound, ToDoubleFunction<T> rightBoundGetter) {
        this.segments = segments;
        this.leftBound = leftBound;
        this.rightBound = rightBoundGetter.applyAsDouble(segments.get(segments.size() - 1));
        this.rightBoundGetter = rightBoundGetter;

        this.currentIndex = 0;
        this.currentLeft = leftBound;
        this.currentRight = this.rightBoundGetter.applyAsDouble(segments.get(0));
        if (leftBound > this.currentRight || leftBound > this.rightBound || this.currentRight > this.rightBound) {
            throw new IllegalArgumentException();
        }
    }

    public T search(final double point) {
        if (segments.size() == 1) {
            return segments.get(0);
        }

        if (point < currentLeft) {
            var firstSegment = segments.get(0);
            if (leftBound == currentLeft) {
                return firstSegment;
            }

            currentIndex = 0;
            currentLeft = leftBound;
            currentRight = rightBoundGetter.applyAsDouble(firstSegment);
            if (point >= currentRight) {
                return search(point);
            } else {
                return firstSegment;
            }
        }

        if (point < currentRight || currentRight == rightBound) {
            return segments.get(currentIndex);
        }

        if (point >= rightBound) {
            var lastSegment = segments.get(segments.size() - 1);
            var left = rightBoundGetter.applyAsDouble(segments.get(segments.size() - 2));
            var right = rightBoundGetter.applyAsDouble(lastSegment);
            if (left > right) {
                throw new IllegalArgumentException();
            }
            currentIndex = segments.size() - 1;
            currentLeft = left;
            currentRight = right;
            return lastSegment;
        }

        double left = currentRight;
        for (int index = currentIndex + 1; ; ++index) {
            var segment = segments.get(index);
            var right = rightBoundGetter.applyAsDouble(segment);
            if (left > right) {
                throw new IllegalArgumentException();
            }
            if (point < right) {
                currentIndex = index;
                currentLeft = left;
                currentRight = right;
                return segment;
            }
            left = right;
        }
    }

    public double leftBound() {
        return leftBound;
    }

    public double rightBound() {
        return rightBound;
    }
}
