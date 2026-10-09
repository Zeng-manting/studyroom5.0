package com.studyroom.web.controller;

import com.studyroom.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 冒烟测试接口：验证骨架启动、统一响应格式、Knife4j 文档是否正常
 */
@Api(tags = "00-冒烟测试")
@RestController
@RequestMapping("/ping")
public class PingController {

    @ApiOperation("服务健康检查（验证统一响应结构）")
    @GetMapping
    public Result<String> ping() {
        return Result.ok("pong");
    }
}
