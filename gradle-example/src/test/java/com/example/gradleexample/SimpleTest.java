package com.example.gradleexample;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Bruce.Wu
 * @date 2024-06-24
 */
class SimpleTest {

    @Test
    void test() {
        Assertions.assertTrue(Math.max(3, 5) == 5);
    }

}
