package com.elfmcys.yesstevemodel.geckolib3.core.controller.transition;

import com.elfmcys.yesstevemodel.geckolib3.util.OrderedSegmentSearcher;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;

import java.util.List;

// Native Access
@SuppressWarnings("unused")
public class SegmentedBlendTransition implements IBlendTransition {
    private final List<Segment> segments;
    private final OrderedSegmentSearcher<Segment> segmentSearcher;

    // Native Access
    public SegmentedBlendTransition(float[] time, float[] position) {
        var segments = new ReferenceArrayList<Segment>(time.length - 1);
        for (int i = 0; i < time.length - 1; i++) {
            segments.add(new Segment(time[i] * 20, time[i + 1] * 20, 1 - position[i], 1 - position[i + 1]));
        }

        this.segments = segments;
        this.segmentSearcher = new OrderedSegmentSearcher<>(this.segments, 0, s -> s.endTick);
    }

    private SegmentedBlendTransition(List<Segment> segments) {
        this.segments = segments;
        this.segmentSearcher = new OrderedSegmentSearcher<>(this.segments, 0, s -> s.endTick);
    }

    @Override
    public double get(double tick) {
        var segment = segmentSearcher.search(tick);
        if (tick <= segment.startTick) {
            return segment.startPosition;
        } else if (tick >= segment.endTick) {
            return segment.startPosition + segment.positionDelta;
        }
        var progress = (tick - segment.startTick) / segment.totalTick;
        return segment.startPosition + segment.positionDelta * progress;
    }

    @Override
    public double length() {
        return segmentSearcher.rightBound();
    }

    @Override
    public SegmentedBlendTransition startNew() {
        return new SegmentedBlendTransition(segments);
    }

    private static class Segment {
        public final double startTick;
        public final double totalTick;
        public final double endTick;
        public final double startPosition;
        public final double positionDelta;

        public Segment(double startTick, double endTick, double startPosition, double endPosition) {
            this.startTick = startTick;
            this.totalTick = endTick - startTick;
            this.endTick = endTick;
            this.startPosition = startPosition;
            this.positionDelta = endPosition - startPosition;
        }
    }
}
