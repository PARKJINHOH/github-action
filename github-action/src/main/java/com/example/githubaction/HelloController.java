package com.example.githubaction;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {


    @GetMapping
    public String Hello() {
        return "Hello Controller";
    }

    @GetMapping("/test")
    public String Test() {
        return "Hello Controller : Test Method";
    }

    @GetMapping("cache")
    public String CacheTest() {
        return "Hello Controller : CacheTest";
    }
}
