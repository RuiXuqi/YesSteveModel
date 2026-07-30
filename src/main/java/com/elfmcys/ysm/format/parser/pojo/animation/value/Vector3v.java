package com.elfmcys.ysm.format.parser.pojo.animation.value;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;
import java.util.List;

@JsonAdapter(Vector3v.Adapter.class)
public class Vector3v {
    public List<UnionValue> components;

    public Vector3v(UnionValue scalar) {
        components = List.of(scalar);
    }

    public Vector3v(UnionValue x, UnionValue y, UnionValue z) {
        components = List.of(x, y, z);
    }

    public static final class Adapter implements JsonDeserializer<Vector3v> {
        @Override
        public Vector3v deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            if (json == null || json.isJsonNull()) {
                throw new JsonParseException("Invalid vector: null");
            }
            if (json.isJsonArray()) {
                var array = json.getAsJsonArray();
                if (array.size() == 3) {
                    return new Vector3v(
                            context.deserialize(array.get(0), UnionValue.class),
                            context.deserialize(array.get(1), UnionValue.class),
                            context.deserialize(array.get(2), UnionValue.class)
                    );
                }
                if (array.size() == 1) {
                    var value = array.get(0);
                    return new Vector3v(
                            context.deserialize(value, UnionValue.class)
                    );
                }
            } else if (json.isJsonPrimitive()) {
                return new Vector3v(
                        context.deserialize(json, UnionValue.class)
                );
            }
            throw new JsonParseException("Invalid vector: " + json);
        }
    }
}
