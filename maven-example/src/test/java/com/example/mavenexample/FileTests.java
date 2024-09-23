package com.example.mavenexample;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.net.URL;

public class FileTests {

    @Test
    public void testFile() {
        File file = new File("https://www.baidu.com");
        URI.create("https://www.baidu.com").getScheme();
    }
}
