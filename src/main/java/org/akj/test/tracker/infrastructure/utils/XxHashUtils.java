package org.akj.test.tracker.infrastructure.utils;

import net.jpountz.xxhash.XXHashFactory;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class XxHashUtils {
    private static final XXHashFactory xxHashFactory = XXHashFactory.fastestInstance();
    private static final int SEED = 0x9747b28c;

    public static String hash(String content) {
        Objects.nonNull(content);

        final byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        return Long.toHexString(xxHashFactory.hash64().hash(bytes, 0, bytes.length, SEED));
    }
}
