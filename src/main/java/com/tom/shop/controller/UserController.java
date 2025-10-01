package com.tom.shop.controller;

import com.tom.shop.service.RedisService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/test")
public class UserController {

    @Resource
    private RedisService redisService;

    @PostMapping("/user")
    public String saveUser() {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("name", "Alice");
        userInfo.put("age", 30);
        userInfo.put("email", "alice@example.com");

        redisService.hSetAll("user:1001", userInfo);
        redisService.expire("user:1001", 30, TimeUnit.MINUTES); // 30分钟过期

        return "OK";
    }

    @GetMapping("/user/{id}")
    public Map<String, Object> getUser(@PathVariable String id) {
        return redisService.hGetAll("user:" + id);
    }
}
