package com.example.gradleexample.controller;

import com.example.gradleexample.model.HelloRequest;
import com.example.gradleexample.model.HelloResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "HelloController", description = "Hello测试接口")
@RestController
@RequestMapping("/api/hello")
public class HelloController {

    @PostMapping("/say")
    @Operation(summary = "Say", description = "打招呼")
    public HelloResponse say(@RequestBody HelloRequest request) {
        HelloResponse response = new HelloResponse();
        response.setWord("Hello, " + request.getName());
        return response;
    }


}
