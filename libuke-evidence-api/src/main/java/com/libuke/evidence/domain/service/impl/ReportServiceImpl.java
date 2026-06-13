package com.libuke.evidence.domain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libuke.evidence.api.dto.CreateReportJsonRequest;
import com.libuke.evidence.api.dto.CreateReportRequest;
import com.libuke.evidence.api.dto.DirectUploadPolicyResponse;
import com.libuke.evidence.api.dto.PageResponse;
import com.libuke.evidence.api.dto.ReportAttachmentResponse;
import com.libuke.evidence.api.dto.ReportImageEvidenceRequest;
import com.libuke.evidence.api.dto.ReportImageUploadResponse;
import com.libuke.evidence.api.dto.ReportResponse;
import com.libuke.evidence.common.BusinessException;
import com.libuke.evidence.domain.entity.Community;
import com.libuke.evidence.domain.entity.Report;
import com.libuke.evidence.domain.entity.ReportEvent;
import com.libuke.evidence.domain.entity.ReportImage;
import com.libuke.evidence.domain.entity.WxUser;
import com.libuke.evidence.domain.mapper.CommunityMapper;
import com.libuke.evidence.domain.mapper.ReportEventMapper;
import com.libuke.evidence.domain.mapper.ReportImageMapper;
import com.libuke.evidence.domain.mapper.ReportMapper;
import com.libuke.evidence.domain.mapper.WxUserMapper;
import com.libuke.evidence.domain.service.ReportService;
import com.libuke.evidence.domain.service.ReportStreamService;
import com.libuke.evidence.domain.service.RuntimeConfigService;
import com.libuke.evidence.integration.map.GeoCodingService;
import com.libuke.evidence.integration.oss.OssUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 问题上报服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final DateTimeFormatter REPORT_NO_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final TypeReference<List<List<BigDecimal>>> COMMUNITY_BOUNDARY_TYPE = new TypeReference<>() {
    };

    private final WxUserMapper wxUserMapper;
    private final CommunityMapper communityMapper;
    private final ReportMapper reportMapper;
    private final ReportEventMapper reportEventMapper;
    private final ReportImageMapper reportImageMapper;
    private final OssUploadService ossUploadService;
    private final GeoCodingService geoCodingService;
    private final RuntimeConfigService runtimeConfigService;
    private final ReportStreamService reportStreamService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportResponse createReport(CreateReportRequest request) {
        long startedAt = System.currentTimeMillis();
        validateFiles(request.getFiles());
        List<String> objectKeys = new ArrayList<>();
        try {
            for (MultipartFile file : request.getFiles()) {
                objectKeys.add(ossUploadService.uploadReportImage(file));
            }
            ReportResponse response = createReportFromObjectKeys(
                request.getOpenid(),
                request.getCategory(),
                request.getSubCategory(),
                request.getLongitude(),
                request.getLatitude(),
                request.getRemark(),
                objectKeys,
                null
            );
            log.info("[report-create] completed costMs={}, reportId={}", elapsed(startedAt), response.getId());
            return response;
        } catch (RuntimeException exception) {
            ossUploadService.deleteObjects(objectKeys);
            throw exception;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportResponse createReport(CreateReportJsonRequest request) {
        validateImageObjectKeys(request.getImageObjectKeys());
        long startedAt = System.currentTimeMillis();
        try {
            ReportResponse response = createReportFromObjectKeys(
                request.getOpenid(),
                request.getCategory(),
                request.getSubCategory(),
                request.getLongitude(),
                request.getLatitude(),
                request.getRemark(),
                request.getImageObjectKeys(),
                buildImageMetadataList(request)
            );
            log.info("[report-create-json] completed costMs={}, reportId={}", elapsed(startedAt), response.getId());
            return response;
        } catch (RuntimeException exception) {
            ossUploadService.deleteObjects(request.getImageObjectKeys());
            throw exception;
        }
    }

    @Override
    public ReportImageUploadResponse uploadReportImage(String openid, MultipartFile file) {
        findVerifiedUser(openid);
        String objectKey = ossUploadService.uploadReportImage(file);
        return ReportImageUploadResponse.builder()
            .objectKey(objectKey)
            .url(ossUploadService.generateTemporaryUrl(objectKey))
            .build();
    }

    @Override
    public DirectUploadPolicyResponse createReportImageDirectUploadPolicy(String openid, String fileName) {
        findVerifiedUser(openid);
        OssUploadService.DirectUploadPolicy policy = ossUploadService.createReportImageDirectUploadPolicy(fileName);
        return DirectUploadPolicyResponse.builder()
            .host(policy.host())
            .objectKey(policy.objectKey())
            .accessKeyId(policy.accessKeyId())
            .policy(policy.policy())
            .signature(policy.signature())
            .successActionStatus(policy.successActionStatus())
            .expireAt(policy.expireAt())
            .build();
    }

    private ReportResponse createReportFromObjectKeys(
        String openid,
        String category,
        String subCategory,
        BigDecimal longitude,
        BigDecimal latitude,
        String remark,
        List<String> objectKeys,
        List<ReportImageEvidenceRequest> imageMetadataList
    ) {
        WxUser user = findVerifiedUser(openid);
        Community community = communityMapper.selectById(user.getCommunityId());
        if (community == null) {
            throw new BusinessException("请先绑定小区");
        }

        validateReportLocationInsideCommunity(community, longitude, latitude);

        Report report = new Report();
        report.setReportNo(generateReportNo());
        report.setUserId(user.getId());
        report.setCommunityId(community.getId());
        report.setCategory(category);
        report.setSubCategory(subCategory);
        report.setLongitude(longitude);
        report.setLatitude(latitude);
        String locationAddress = trimToNull(geoCodingService.reverseGeocode(longitude, latitude));
        report.setLocationAddress(StringUtils.hasText(locationAddress) ? locationAddress : "未知地址");
        report.setRemark(trimToNull(remark));
        report.setStatus("pending");
        report.setSubmittedAt(LocalDateTime.now());
        reportMapper.insert(report);
        createReportEvent(report, user);
        publishReportCreatedAfterCommit(report);

        for (int index = 0; index < objectKeys.size(); index++) {
            String objectKey = objectKeys.get(index);
            ReportImageEvidenceRequest metadata = resolveImageMetadata(objectKey, imageMetadataList);
            ReportImage image = new ReportImage();
            image.setReportId(report.getId());
            image.setOriginalObjectKey(objectKey);
            image.setSortOrder(index);
            image.setProcessStatus("original_only");
            image.setClientUploadedAt(parseClientUploadedAt(metadata.getClientUploadedAt()));
            image.setOriginalFileName(trimToNull(metadata.getOriginalFileName()));
            image.setServerReceivedAt(LocalDateTime.now());
            reportImageMapper.insert(image);
            fillImageInfo(image);
        }

        return toResponse(report, community.getName());
    }

    private void createReportEvent(Report report, WxUser user) {
        ReportEvent event = new ReportEvent();
        event.setReportId(report.getId());
        event.setEventType("created");
        event.setToStatus(report.getStatus());
        event.setOperatorType("miniapp");
        event.setOperatorId(user.getId());
        event.setOperatorName(StringUtils.hasText(user.getWitnessInfo()) ? user.getWitnessInfo() : "业主用户");
        event.setContent("业主提交问题上报");
        event.setCreatedAt(report.getSubmittedAt());
        reportEventMapper.insert(event);
    }

    private void publishReportCreatedAfterCommit(Report report) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            reportStreamService.publishReportCreated(report);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                reportStreamService.publishReportCreated(report);
            }
        });
    }

    @Override
    public PageResponse<ReportResponse> pageMyReports(String openid, String category, long pageNo, long pageSize) {
        WxUser user = findVerifiedUser(openid);
        Page<Report> page = reportMapper.selectPage(
            Page.of(pageNo, Math.min(pageSize, 50)),
            new LambdaQueryWrapper<Report>()
                .eq(Report::getUserId, user.getId())
                .eq(StringUtils.hasText(category), Report::getCategory, category)
                .orderByDesc(Report::getSubmittedAt)
        );
        Community community = communityMapper.selectById(user.getCommunityId());
        String communityName = community == null ? null : community.getName();

        return PageResponse.<ReportResponse>builder()
            .total(page.getTotal())
            .pageNo(page.getCurrent())
            .pageSize(page.getSize())
            .records(page.getRecords().stream().map(report -> toResponse(report, communityName)).toList())
            .build();
    }

    @Override
    public ReportResponse getMyReport(String openid, Long reportId) {
        WxUser user = findVerifiedUser(openid);
        Report report = reportMapper.selectById(reportId);
        if (report == null || !user.getId().equals(report.getUserId())) {
            throw new BusinessException("记录不存在");
        }

        Community community = communityMapper.selectById(report.getCommunityId());
        return toResponse(report, community == null ? null : community.getName());
    }

    private ReportResponse toResponse(Report report, String communityName) {
        List<ReportImage> images = reportImageMapper.selectList(
            new LambdaQueryWrapper<ReportImage>()
                .eq(ReportImage::getReportId, report.getId())
                .orderByAsc(ReportImage::getSortOrder)
        );
        String firstImageObjectKey = images.isEmpty() ? null : images.getFirst().getOriginalObjectKey();
        List<ReportAttachmentResponse> attachments = images.stream()
            .map(image -> ReportAttachmentResponse.builder()
                .id(image.getId())
                .objectKey(image.getOriginalObjectKey())
                .url(ossUploadService.generateTemporaryUrl(image.getOriginalObjectKey()))
                .type("image")
                .sortOrder(image.getSortOrder())
                .originalFileSize(image.getOriginalFileSize())
                .originalMimeType(image.getOriginalMimeType())
                .originalFileName(image.getOriginalFileName())
                .imageWidth(image.getImageWidth())
                .imageHeight(image.getImageHeight())
                .clientUploadedAt(image.getClientUploadedAt())
                .serverReceivedAt(image.getServerReceivedAt())
                .build())
            .toList();
        return ReportResponse.builder()
            .id(report.getId())
            .reportNo(report.getReportNo())
            .communityName(communityName)
            .category(report.getCategory())
            .subCategory(report.getSubCategory())
            .remark(report.getRemark())
            .status(report.getStatus())
            .longitude(report.getLongitude())
            .latitude(report.getLatitude())
            .locationAddress(report.getLocationAddress())
            .firstImageObjectKey(firstImageObjectKey)
            .firstImageUrl(ossUploadService.generateTemporaryUrl(firstImageObjectKey))
            .imageCount(images.size())
            .attachments(attachments)
            .submittedAt(report.getSubmittedAt())
            .build();
    }

    private WxUser findVerifiedUser(String openid) {
        WxUser user = wxUserMapper.selectOne(
            new LambdaQueryWrapper<WxUser>()
                .eq(WxUser::getOpenid, openid)
                .last("limit 1")
        );
        if (user == null || user.getCommunityId() == null || !"verified".equals(user.getAuthStatus())) {
            throw new BusinessException("请先绑定小区");
        }
        return user;
    }

    private void validateReportLocationInsideCommunity(Community community, BigDecimal longitude, BigDecimal latitude) {
        List<List<BigDecimal>> boundary = parseCommunityBoundary(community.getBoundaryJson());
        if (boundary == null || boundary.size() < 3 || longitude == null || latitude == null) {
            return;
        }
        if (!isPointInPolygon(longitude.doubleValue(), latitude.doubleValue(), boundary)) {
            throw new BusinessException("上报位置不在当前小区范围内，请重新选择位置");
        }
    }

    private List<List<BigDecimal>> parseCommunityBoundary(String boundaryJson) {
        if (!StringUtils.hasText(boundaryJson)) {
            return null;
        }
        try {
            return objectMapper.readValue(boundaryJson, COMMUNITY_BOUNDARY_TYPE);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private boolean isPointInPolygon(double longitude, double latitude, List<List<BigDecimal>> boundary) {
        boolean inside = false;
        int pointCount = boundary.size();
        for (int index = 0, previousIndex = pointCount - 1; index < pointCount; previousIndex = index++) {
            List<BigDecimal> current = boundary.get(index);
            List<BigDecimal> previous = boundary.get(previousIndex);
            if (current == null || previous == null || current.size() != 2 || previous.size() != 2
                || current.get(0) == null || current.get(1) == null || previous.get(0) == null || previous.get(1) == null) {
                return true;
            }
            double currentLng = current.get(0).doubleValue();
            double currentLat = current.get(1).doubleValue();
            double previousLng = previous.get(0).doubleValue();
            double previousLat = previous.get(1).doubleValue();
            if (isPointOnSegment(longitude, latitude, previousLng, previousLat, currentLng, currentLat)) {
                return true;
            }
            boolean intersect = ((currentLat > latitude) != (previousLat > latitude))
                && (longitude < (previousLng - currentLng) * (latitude - currentLat) / (previousLat - currentLat) + currentLng);
            if (intersect) {
                inside = !inside;
            }
        }
        return inside;
    }

    private boolean isPointOnSegment(
        double longitude,
        double latitude,
        double startLng,
        double startLat,
        double endLng,
        double endLat
    ) {
        double tolerance = 0.0000001;
        double cross = (longitude - startLng) * (endLat - startLat) - (latitude - startLat) * (endLng - startLng);
        if (Math.abs(cross) > tolerance) {
            return false;
        }
        return longitude >= Math.min(startLng, endLng) - tolerance
            && longitude <= Math.max(startLng, endLng) + tolerance
            && latitude >= Math.min(startLat, endLat) - tolerance
            && latitude <= Math.max(startLat, endLat) + tolerance;
    }

    private List<ReportImageEvidenceRequest> buildImageMetadataList(CreateReportJsonRequest request) {
        if (request.getImageEvidenceList() != null && !request.getImageEvidenceList().isEmpty()) {
            return request.getImageEvidenceList();
        }
        return request.getImageObjectKeys().stream()
            .map(objectKey -> {
                ReportImageEvidenceRequest metadata = new ReportImageEvidenceRequest();
                metadata.setObjectKey(objectKey);
                return metadata;
            })
            .toList();
    }

    private ReportImageEvidenceRequest resolveImageMetadata(String objectKey, List<ReportImageEvidenceRequest> imageMetadataList) {
        if (imageMetadataList != null) {
            for (ReportImageEvidenceRequest metadata : imageMetadataList) {
                if (metadata != null && objectKey.equals(metadata.getObjectKey())) {
                    return metadata;
                }
            }
        }
        ReportImageEvidenceRequest fallback = new ReportImageEvidenceRequest();
        fallback.setObjectKey(objectKey);
        return fallback;
    }

    private void fillImageInfo(ReportImage image) {
        long startedAt = System.currentTimeMillis();
        OssUploadService.StoredObject storedObject = ossUploadService.loadObject(image.getOriginalObjectKey());
        ImageSize imageSize = readImageSize(storedObject.content());

        image.setOriginalFileSize(storedObject.contentLength());
        image.setOriginalMimeType(trimToNull(storedObject.contentType()));
        image.setImageWidth(imageSize.width());
        image.setImageHeight(imageSize.height());
        image.setServerReceivedAt(LocalDateTime.now());
        reportImageMapper.updateById(image);
        log.info("[report-create] image info filled costMs={}, imageId={}", elapsed(startedAt), image.getId());
    }

    private ImageSize readImageSize(byte[] content) {
        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            if (imageInputStream == null) {
                return new ImageSize(null, null);
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
            if (!readers.hasNext()) {
                return new ImageSize(null, null);
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInputStream, true, true);
                return new ImageSize(reader.getWidth(0), reader.getHeight(0));
            } finally {
                reader.dispose();
            }
        } catch (Exception exception) {
            return new ImageSize(null, null);
        }
    }

    private LocalDateTime parseClientUploadedAt(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.ofInstant(Instant.parse(value.trim()), ZoneId.systemDefault());
        } catch (Exception ignored) {
            try {
                return OffsetDateTime.parse(value.trim()).toLocalDateTime();
            } catch (Exception exception) {
                return null;
            }
        }
    }

    private String generateReportNo() {
        return "R" + LocalDateTime.now().format(REPORT_NO_TIME_FORMAT) + System.nanoTime();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void validateFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new BusinessException("请至少上传 1 张图片");
        }
        RuntimeConfigService.UploadPolicyRuntimeConfig policy = runtimeConfigService.uploadPolicy("report_image");
        int maxFileCount = policy.maxFileCount() == null ? 4 : policy.maxFileCount();
        if (files.size() > maxFileCount) {
            throw new BusinessException("最多上传 " + maxFileCount + " 张图片");
        }
    }

    private void validateImageObjectKeys(List<String> imageObjectKeys) {
        if (imageObjectKeys == null || imageObjectKeys.isEmpty()) {
            throw new BusinessException("请至少上传 1 张图片");
        }
        RuntimeConfigService.UploadPolicyRuntimeConfig policy = runtimeConfigService.uploadPolicy("report_image");
        int maxFileCount = policy.maxFileCount() == null ? 4 : policy.maxFileCount();
        if (imageObjectKeys.size() > maxFileCount) {
            throw new BusinessException("最多上传 " + maxFileCount + " 张图片");
        }
        boolean hasBlankObjectKey = imageObjectKeys.stream().anyMatch(objectKey -> !StringUtils.hasText(objectKey));
        if (hasBlankObjectKey) {
            throw new BusinessException("图片上传结果不能为空");
        }
        boolean hasInvalidObjectKey = imageObjectKeys.stream().anyMatch(objectKey -> !ossUploadService.isReportImageObjectKey(objectKey));
        if (hasInvalidObjectKey) {
            throw new BusinessException("图片上传结果无效");
        }
    }

    private long elapsed(long startedAt) {
        return System.currentTimeMillis() - startedAt;
    }

    private record ImageSize(Integer width, Integer height) {
    }
}
