package com.example.gradleexample.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Hello请求", description = "Hello请求")
public class HelloRequest {

    @Schema(name = "name", description = "名字", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
