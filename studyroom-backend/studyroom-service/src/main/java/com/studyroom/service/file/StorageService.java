package com.studyroom.service.file;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务接口
 *
 * <p>FR-CC-003 当前替代方案：本地文件存储；
 * 后续开通腾讯云 COS 后切换实现（StorageService 另行实现，接口不变）。</p>
 *
 * @author studyroom
 */
public interface StorageService {

    /**
     * 上传图片文件
     *
     * @param file  前端上传的文件（uni.uploadFile）
     * @param scene 业务场景：avatar（头像）/ review（评价）/ feedback（反馈）
     * @return 可访问的图片 URL
     */
    String uploadImage(MultipartFile file, String scene);
}
