package com.studyroom.service.file;

import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 本地文件存储实现 —— FR-CC-003 当前替代方案
 *
 * <h3>安全设计</h3>
 * <ul>
 *   <li>白名单校验扩展名与 Content-Type（jpg/png/webp），防上传脚本伪装</li>
 *   <li>存储文件名服务端生成（UUID），不信任原始文件名，防路径穿越</li>
 *   <li>按 业务场景/日期 分目录，便于备份与清理</li>
 * </ul>
 *
 * <p>访问路径：/api/file/{scene}/{date}/{uuid}.jpg 由 WebMvcConfig 静态映射到存储目录。</p>
 *
 * @author studyroom
 */
@Slf4j
@Service
public class LocalStorageServiceImpl implements StorageService {

    private static final List<String> ALLOWED_EXT = Arrays.asList("jpg", "jpeg", "png", "webp");
    private static final List<String> ALLOWED_MIME = Arrays.asList(
            "image/jpeg", "image/png", "image/webp");

    private static final long MAX_SIZE = 5 * 1024 * 1024L;

    @Value("${studyroom.storage.dir:./storage}")
    private String storageDir;

    @Value("${studyroom.storage.url-prefix:/api/file}")
    private String urlPrefix;

    @Override
    public String uploadImage(MultipartFile file, String scene) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "上传文件不能为空");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "图片大小不能超过 5MB");
        }
        // 场景白名单（目录名即场景名，防任意目录写入）
        if (!Arrays.asList("avatar", "review", "feedback").contains(scene)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的上传场景");
        }

        String ext = resolveExt(file.getOriginalFilename());
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXT.contains(ext) || !ALLOWED_MIME.contains(contentType)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "仅支持 jpg/png/webp 格式图片");
        }

        // 存储路径：{dir}/{scene}/{yyyyMMdd}/{uuid}.{ext}
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path target;
        try {
            Path dir = Paths.get(storageDir, scene, dateDir);
            Files.createDirectories(dir);
            target = dir.resolve(filename);
            file.transferTo(target.toFile());
        } catch (IOException e) {
            log.error("[LocalStorageServiceImpl] 文件写入失败: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.ERROR, "文件上传失败，请重试");
        }

        String url = urlPrefix + "/" + scene + "/" + dateDir + "/" + filename;
        log.info("[LocalStorageServiceImpl] 图片上传成功 scene={} size={} url={}",
                scene, file.getSize(), url);
        return url;
    }

    /** 提取小写扩展名（不含点），非法字符直接拒绝 */
    private String resolveExt(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
