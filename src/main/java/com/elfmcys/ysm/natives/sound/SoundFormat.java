package com.elfmcys.ysm.natives.sound;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceLists;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public enum SoundFormat {
    PCM(0),
    OGG_VORBIS(1),
    OGG_OPUS(2);

    private final int id;

    SoundFormat(int value) {
        id = value;
    }

    public int id() {
        return id;
    }

    public static final List<SoundFormat> VALUES = ReferenceLists.unmodifiable(ReferenceArrayList.wrap(
            Arrays.stream(SoundFormat.values()).sorted(Comparator.comparingInt(f -> f.id)).toArray(SoundFormat[]::new)));
}
