package com.elfmcys.ysm.model.catalog;

import mixel.manifest.asset.RenderTargetOuterClass;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;

/** Stable compatibility identity for one default-model animation. */
public record DefaultAnimationKey(String domain, String name)
        implements Comparable<DefaultAnimationKey> {
    public DefaultAnimationKey {
        if (Objects.requireNonNull(domain, "domain").isBlank()) {
            throw new IllegalArgumentException("Animation domain is empty");
        }
        if (Objects.requireNonNull(name, "name").isBlank()) {
            throw new IllegalArgumentException("Animation name is empty");
        }
    }

    public static String domain(RenderTargetOuterClass.RenderTarget target,
                                String animationSet) {
        var kind = target.getKind();
        if (kind == RenderTargetOuterClass.RenderTargetKind.RENDER_TARGET_KIND_PLAYER) {
            return animationSet.equals("fp_arm")
                    ? "player/first-person" : "player/main";
        }
        var matches = new ArrayList<String>();
        if (target.hasMatch()) {
            target.getMatch().forEach(matches::add);
        }
        matches.sort(Comparator.naturalOrder());
        var canonical = String.join("\0", matches);
        var suffix = java.util.HexFormat.of().formatHex(
                sha256(canonical.getBytes(StandardCharsets.UTF_8)), 0, 8);
        return kind.name().toLowerCase(java.util.Locale.ROOT) + "/" + suffix + "/main";
    }

    @Override
    public int compareTo(DefaultAnimationKey other) {
        var order = domain.compareTo(other.domain);
        return order != 0 ? order : name.compareTo(other.name);
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException error) {
            throw new AssertionError(error);
        }
    }
}
