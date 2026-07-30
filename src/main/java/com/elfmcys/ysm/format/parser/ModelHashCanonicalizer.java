package com.elfmcys.ysm.format.parser;

import com.elfmcys.ysm.buffer.ArrayBuffer;
import com.elfmcys.ysm.natives.Blake3;
import it.unimi.dsi.fastutil.objects.Object2ReferenceRBTreeMap;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ModelHashCanonicalizer {
    private Object2ReferenceRBTreeMap<String, String> items = new Object2ReferenceRBTreeMap<>();

    public void add(String type, String filePath, byte[] hash) {
        items.put(String.format("%s|%s", type, filePath.replace("\n", "\\n")), Base64.getEncoder().encodeToString(hash));
    }

    public byte[] aggregate() {
        var sb = new StringBuilder();
        for (var entry : items.entrySet()) {
            sb.append(entry.getKey());
            sb.append('|');
            sb.append(entry.getValue());
            sb.append('\n');
        }
        return Blake3.computeHash(ArrayBuffer.move(sb.toString().getBytes(StandardCharsets.UTF_8)));
    }
}
