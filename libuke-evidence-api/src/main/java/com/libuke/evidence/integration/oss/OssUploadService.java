package com.libuke.evidence.integration.oss;

import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;

public interface OssUploadService {

    String uploadAvatarImage(MultipartFile file);

    String uploadReportImage(MultipartFile file);

    DirectUploadPolicy createReportImageDirectUploadPolicy(String fileName);

    boolean isReportImageObjectKey(String objectKey);

    StoredObject loadObject(String objectKey);

    String uploadWatermarkedReportImage(MultipartFile file, String originalObjectKey, List<String> watermarkLines);

    String generateWatermarkedReportImage(String originalObjectKey, List<String> watermarkLines, WatermarkRenderOptions options);

    String generateTemporaryUrl(String objectKey);

    void deleteObjects(List<String> objectKeys);

    record DirectUploadPolicy(
        String host,
        String objectKey,
        String accessKeyId,
        String policy,
        String signature,
        String successActionStatus,
        Long expireAt
    ) {
    }

    record StoredObject(
        String objectKey,
        byte[] content,
        Long contentLength,
        String contentType,
        String etag,
        Date lastModified
    ) {
    }
}
