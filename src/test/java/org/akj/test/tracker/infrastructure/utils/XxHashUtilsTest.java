package org.akj.test.tracker.infrastructure.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XxHashUtilsTest {

    @Test
    void hash() {
        String content = "test";
        String expectedHash = "e118f665ce62a847";

        String actualHash = XxHashUtils.hash(content);

        assertEquals(expectedHash, actualHash);
    }

    @Test
    void hashNullContent() {
        assertThrows(NullPointerException.class, () -> XxHashUtils.hash(null));
    }
}
