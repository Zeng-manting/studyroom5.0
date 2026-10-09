package com.studyroom.web.controller;

import com.studyroom.common.annotation.RateLimiter;
import com.studyroom.common.result.Result;
import com.studyroom.service.file.StorageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

/**
 * 文件上传 Controller —— FR-CC-003（当前本地存储替代云存储）
 *
 * <h3>接口清单</h3>
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>说明</th><th>FR</th></tr>
 *   <tr><td>POST</td><td>/api/file/upload</td><td>图片上传（头像/评价/反馈）</td><td>FR-UC-003/039/052</td></tr>
 * </table>
 *
 * <p>前端使用 uni.uploadFile；返回 URL 后再调用业务接口（如 PUT /api/user/profile）。</p>
 *
 * @author studyroom
 */
@Slf4j
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
@Api(tags = "文件上传")
public class FileController {

    private final StorageService storageService;

    /**
     * 图片上传（需登录）
     *
     * @param file  multipart 文件
     * @param scene 业务场景：avatar（头像，FR-UC-003）/ review（评价图，FR-UC-039）/ feedback（反馈图，FR-UC-052）
     */
    @PostMapping("/upload")
    @ApiOperation("上传图片（jpg/png/webp，≤5MB），返回可访问 URL")
    @RateLimiter(value = "file:upload", permits = 10, interval = 1,
            unit = TimeUnit.MINUTES, key = RateLimiter.KeyType.USER)
    public Result<String> upload(
            @ApiParam(value = "图片文件", required = true) @RequestParam("file") MultipartFile file,
            @ApiParam(value = "业务场景：avatar/review/feedback", required = true) @RequestParam("scene") String scene) {
        return Result.ok(storageService.uploadImage(file, scene));
    }
}
