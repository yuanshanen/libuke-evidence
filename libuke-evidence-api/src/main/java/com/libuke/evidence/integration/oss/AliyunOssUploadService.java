package com.libuke.evidence.integration.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.model.MatchMode;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PolicyConditions;
import com.libuke.evidence.common.BusinessException;
import com.libuke.evidence.config.WatermarkProperties;
import com.libuke.evidence.domain.service.RuntimeConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class AliyunOssUploadService implements OssUploadService {

    private static final int WATERMARK_REFERENCE_WIDTH = 720;
    private static final float WATERMARK_JPEG_QUALITY = 0.95f;
    private static final String FONT_SAMPLE = "物业服务不达标取证小程序水印位置上报时间编号未知地址";
    private static final List<String> DEFAULT_CHINESE_FONT_NAMES = List.of(
        "Noto Sans CJK SC",
        "Source Han Sans SC",
        "Microsoft YaHei",
        "SimHei",
        "WenQuanYi Micro Hei",
        "Dialog"
    );

    private final RuntimeConfigService runtimeConfigService;
    private final WatermarkProperties watermarkProperties;

    @Override
    public String uploadAvatarImage(MultipartFile file) {
        validateOssConfig();
        validateImageFile(file);
        RuntimeConfigService.StorageRuntimeConfig config = runtimeConfigService.storageConfig();
        String objectKey = buildObjectKey(config.avatarDir(), file.getOriginalFilename() == null ? "avatar.jpg" : file.getOriginalFilename());
        uploadMultipartFile(objectKey, file);
        return objectKey;
    }

    @Override
    public String uploadReportImage(MultipartFile file) {
        validateOssConfig();
        validateImageFile(file);
        RuntimeConfigService.StorageRuntimeConfig config = runtimeConfigService.storageConfig();
        String objectKey = buildObjectKey(config.originalDir(), file.getOriginalFilename() == null ? "image.jpg" : file.getOriginalFilename());
        uploadMultipartFile(objectKey, file);
        return objectKey;
    }

    @Override
    public DirectUploadPolicy createReportImageDirectUploadPolicy(String fileName) {
        validateOssConfig();
        RuntimeConfigService.UploadPolicyRuntimeConfig policy = runtimeConfigService.uploadPolicy("report_image");
        RuntimeConfigService.StorageRuntimeConfig config = runtimeConfigService.storageConfig();
        String objectKey = buildObjectKey(config.originalDir(), StringUtils.hasText(fileName) ? fileName : "image.jpg");
        Date expiration = Date.from(Instant.now().plus(10, ChronoUnit.MINUTES));
        PolicyConditions conditions = new PolicyConditions();
        conditions.addConditionItem(
            PolicyConditions.COND_CONTENT_LENGTH_RANGE,
            1,
            (policy.maxFileSizeMb() == null ? 20 : policy.maxFileSizeMb()) * 1024L * 1024L
        );
        conditions.addConditionItem(MatchMode.StartWith, PolicyConditions.COND_KEY, normalizeDir(config.originalDir()) + "/");

        OSS ossClient = buildOssClient();
        try {
            String postPolicy = ossClient.generatePostPolicy(expiration, conditions);
            String encodedPolicy = BinaryUtil.toBase64String(postPolicy.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return new DirectUploadPolicy(
                buildPublicBucketHost(),
                objectKey,
                accessKeyId(),
                encodedPolicy,
                ossClient.calculatePostSignature(postPolicy),
                "200",
                expiration.getTime()
            );
        } finally {
            ossClient.shutdown();
        }
    }

    @Override
    public boolean isReportImageObjectKey(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return false;
        }
        String originalDir = normalizeDir(runtimeConfigService.storageConfig().originalDir());
        return StringUtils.hasText(originalDir) && objectKey.startsWith(originalDir + "/");
    }

    @Override
    public StoredObject loadObject(String objectKey) {
        validateOssConfig();
        if (!StringUtils.hasText(objectKey)) {
            throw new BusinessException("OSS Key 不能为空");
        }
        OSS ossClient = buildOssClient();
        try {
            ObjectMetadata metadata = ossClient.getObjectMetadata(bucketName(), objectKey);
            OSSObject ossObject = ossClient.getObject(bucketName(), objectKey);
            try (InputStream objectContent = ossObject.getObjectContent()) {
                return new StoredObject(
                    objectKey,
                    objectContent.readAllBytes(),
                    metadata.getContentLength(),
                    metadata.getContentType(),
                    metadata.getETag(),
                    metadata.getLastModified()
                );
            }
        } catch (IOException exception) {
            throw new BusinessException(500, "读取 OSS 原图失败");
        } finally {
            ossClient.shutdown();
        }
    }

    @Override
    public String uploadWatermarkedReportImage(MultipartFile file, String originalObjectKey, List<String> watermarkLines) {
        validateOssConfig();
        validateImageFile(file);
        if (!StringUtils.hasText(originalObjectKey)) {
            throw new BusinessException(500, "原图 OSS Key 不能为空");
        }
        String objectKey = buildWatermarkedObjectKey(originalObjectKey);
        uploadJpegBytes(objectKey, buildWatermarkedImage(file, watermarkLines));
        return objectKey;
    }

    @Override
    public String generateWatermarkedReportImage(String originalObjectKey, List<String> watermarkLines, WatermarkRenderOptions options) {
        validateOssConfig();
        if (!StringUtils.hasText(originalObjectKey)) {
            throw new BusinessException(500, "原图 OSS Key 不能为空");
        }
        String objectKey = buildWatermarkedObjectKey(originalObjectKey);
        OSS ossClient = buildOssClient();
        try {
            OSSObject ossObject = ossClient.getObject(bucketName(), originalObjectKey);
            uploadJpegBytes(objectKey, buildWatermarkedImage(ossObject.getObjectContent(), watermarkLines, options));
            return objectKey;
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(500, "生成水印图片失败");
        } finally {
            ossClient.shutdown();
        }
    }

    @Override
    public String generateTemporaryUrl(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return null;
        }
        validateOssConfig();
        OSS ossClient = buildOssClient();
        try {
            Integer minutes = runtimeConfigService.storageConfig().presignedUrlMinutes();
            Date expiration = Date.from(Instant.now().plus(minutes == null ? 30 : minutes, ChronoUnit.MINUTES));
            return ossClient.generatePresignedUrl(bucketName(), objectKey, expiration).toString();
        } finally {
            ossClient.shutdown();
        }
    }

    @Override
    public void deleteObjects(List<String> objectKeys) {
        if (objectKeys == null || objectKeys.isEmpty()) {
            return;
        }
        validateOssConfig();
        OSS ossClient = buildOssClient();
        try {
            objectKeys.stream()
                .filter(StringUtils::hasText)
                .forEach(objectKey -> ossClient.deleteObject(bucketName(), objectKey));
        } finally {
            ossClient.shutdown();
        }
    }

    private void uploadMultipartFile(String objectKey, MultipartFile file) {
        OSS ossClient = buildOssClient();
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            ossClient.putObject(bucketName(), objectKey, file.getInputStream(), metadata);
        } catch (IOException exception) {
            throw new BusinessException(500, "图片上传 OSS 失败");
        } finally {
            ossClient.shutdown();
        }
    }

    private void uploadJpegBytes(String objectKey, byte[] imageBytes) {
        OSS ossClient = buildOssClient();
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("image/jpeg");
            ossClient.putObject(bucketName(), objectKey, new ByteArrayInputStream(imageBytes), metadata);
        } finally {
            ossClient.shutdown();
        }
    }

    private String buildObjectKey(String baseDir, String fileName) {
        String suffix = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < fileName.length() - 1) {
            suffix = fileName.substring(dotIndex).toLowerCase();
        }
        LocalDate today = LocalDate.now();
        return "%s/%s/%s/%s%s".formatted(
            normalizeDir(baseDir),
            today.getYear(),
            "%02d".formatted(today.getMonthValue()),
            UUID.randomUUID(),
            suffix
        );
    }

    private String buildWatermarkedObjectKey(String originalObjectKey) {
        RuntimeConfigService.StorageRuntimeConfig config = runtimeConfigService.storageConfig();
        String originalDir = normalizeDir(config.originalDir());
        String watermarkedDir = normalizeDir(config.watermarkedDir());
        String watermarkedObjectKey = originalObjectKey.replace("/" + originalDir + "/", "/" + watermarkedDir + "/");
        if (watermarkedObjectKey.equals(originalObjectKey) && originalObjectKey.startsWith(originalDir + "/")) {
            watermarkedObjectKey = watermarkedDir + originalObjectKey.substring(originalDir.length());
        }
        if (watermarkedObjectKey.equals(originalObjectKey)) {
            watermarkedObjectKey = originalObjectKey.replace("original", "watermarked");
        }
        int dotIndex = watermarkedObjectKey.lastIndexOf('.');
        if (dotIndex >= 0) {
            return watermarkedObjectKey.substring(0, dotIndex) + ".jpg";
        }
        return watermarkedObjectKey + ".jpg";
    }

    private byte[] buildWatermarkedImage(MultipartFile file, List<String> watermarkLines) {
        try {
            return buildWatermarkedImage(file.getInputStream(), watermarkLines, null);
        } catch (IOException exception) {
            throw new BusinessException(500, "生成水印图片失败");
        }
    }

    private byte[] buildWatermarkedImage(InputStream inputStream, List<String> watermarkLines, WatermarkRenderOptions options) {
        try {
            BufferedImage source = ImageIO.read(inputStream);
            if (source == null) {
                throw new BusinessException("图片内容无法识别");
            }
            BufferedImage target = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = target.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.drawImage(source, 0, 0, Color.WHITE, null);
                drawWatermark(graphics, source.getWidth(), source.getHeight(), watermarkLines, options);
            } finally {
                graphics.dispose();
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            writeJpeg(target, outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new BusinessException(500, "生成水印图片失败");
        }
    }

    private void writeJpeg(RenderedImage image, ByteArrayOutputStream outputStream) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            ImageIO.write(image, "jpg", outputStream);
            return;
        }
        ImageWriter writer = writers.next();
        try (MemoryCacheImageOutputStream imageOutputStream = new MemoryCacheImageOutputStream(outputStream)) {
            writer.setOutput(imageOutputStream);
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            if (writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionQuality(WATERMARK_JPEG_QUALITY);
            }
            writer.write(null, new javax.imageio.IIOImage(image, null, null), writeParam);
        } finally {
            writer.dispose();
        }
    }

    private void drawWatermark(Graphics2D graphics, int width, int height, List<String> watermarkLines, WatermarkRenderOptions options) {
        List<String> lines = watermarkLines == null ? List.of() : watermarkLines.stream()
            .filter(StringUtils::hasText)
            .toList();
        if (lines.isEmpty()) {
            return;
        }
        int padding = Math.max(24, width / 42);
        int fontSize = options != null && options.fontSize() != null && options.fontSize() > 0
            ? Math.max(12, Math.round(options.fontSize() * width / (float) WATERMARK_REFERENCE_WIDTH))
            : Math.max(26, width / 42);
        Font font = resolveWatermarkFont(fontSize);
        graphics.setFont(font);
        FontMetrics fontMetrics = graphics.getFontMetrics();
        int lineHeight = fontMetrics.getHeight() + Math.max(6, fontSize / 4);
        int panelHeight = padding * 2 + lineHeight * lines.size();
        int panelY = "top".equals(options == null ? null : options.position()) ? 0 : Math.max(0, height - panelHeight);

        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, floatValue(options == null ? null : options.backgroundOpacity(), 0.58f)));
        graphics.setColor(parseColor(options == null ? null : options.backgroundColor(), Color.BLACK));
        graphics.fillRect(0, panelY, width, panelHeight);
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, floatValue(options == null ? null : options.opacity(), 1f)));
        graphics.setColor(parseColor(options == null ? null : options.textColor(), Color.WHITE));

        int textY = panelY + padding + fontMetrics.getAscent();
        for (String line : lines) {
            graphics.drawString(line, padding, textY);
            textY += lineHeight;
        }
    }

    private float floatValue(BigDecimal value, float defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return Math.max(0f, Math.min(1f, value.floatValue()));
    }

    private Color parseColor(String value, Color defaultColor) {
        if (!StringUtils.hasText(value)) {
            return defaultColor;
        }
        try {
            return Color.decode(value);
        } catch (NumberFormatException exception) {
            return defaultColor;
        }
    }

    private Font resolveWatermarkFont(int fontSize) {
        if (StringUtils.hasText(watermarkProperties.getPath())) {
            File fontFile = new File(watermarkProperties.getPath());
            if (fontFile.isFile()) {
                try {
                    Font font = Font.createFont(Font.TRUETYPE_FONT, fontFile);
                    GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
                    return font.deriveFont(Font.BOLD, (float) fontSize);
                } catch (Exception exception) {
                    log.warn("[watermark] failed to load configured font file: {}", watermarkProperties.getPath(), exception);
                }
            } else {
                log.warn("[watermark] configured font file does not exist: {}", watermarkProperties.getPath());
            }
        }

        if (StringUtils.hasText(watermarkProperties.getName())) {
            Font configuredFont = findInstalledFont(watermarkProperties.getName().trim(), fontSize);
            if (configuredFont != null) {
                return configuredFont;
            }
            log.warn("[watermark] configured font may not display Chinese: {}", watermarkProperties.getName());
        }

        for (String fontName : DEFAULT_CHINESE_FONT_NAMES) {
            Font font = findInstalledFont(fontName, fontSize);
            if (font != null) {
                return font;
            }
        }

        for (Font font : GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts()) {
            if (font.canDisplayUpTo(FONT_SAMPLE) < 0) {
                return font.deriveFont(Font.BOLD, (float) fontSize);
            }
        }

        log.warn("[watermark] no Chinese-capable font detected, falling back to Dialog. Please install Noto Sans CJK SC or set WATERMARK_FONT_PATH");
        return new Font("Dialog", Font.BOLD, fontSize);
    }

    private Font findInstalledFont(String fontName, int fontSize) {
        if (!StringUtils.hasText(fontName)) {
            return null;
        }
        for (Font font : GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts()) {
            if ((font.getFamily().equalsIgnoreCase(fontName) || font.getFontName().equalsIgnoreCase(fontName))
                && font.canDisplayUpTo(FONT_SAMPLE) < 0) {
                return font.deriveFont(Font.BOLD, (float) fontSize);
            }
        }
        return null;
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("图片文件不能为空");
        }
        RuntimeConfigService.UploadPolicyRuntimeConfig policy = runtimeConfigService.uploadPolicy("report_image");
        int maxFileSizeMb = policy.maxFileSizeMb() == null ? 20 : policy.maxFileSizeMb();
        if (file.getSize() > maxFileSizeMb * 1024L * 1024L) {
            throw new BusinessException("单张图片不能超过 " + maxFileSizeMb + "MB");
        }
        String contentType = file.getContentType();
        List<String> allowedTypes = Arrays.stream((policy.allowedMimeTypes() == null ? "image/jpeg,image/png" : policy.allowedMimeTypes()).split(","))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .toList();
        if (!allowedTypes.contains(contentType)) {
            throw new BusinessException("不支持的图片类型");
        }
    }

    private void validateOssConfig() {
        if (!StringUtils.hasText(accessKeyId())
            || !StringUtils.hasText(accessKeySecret())
            || !StringUtils.hasText(bucketName())
            || !StringUtils.hasText(runtimeConfigService.storageConfig().endpoint())) {
            throw new BusinessException(500, "OSS 配置未完成");
        }
    }

    private OSS buildOssClient() {
        return new OSSClientBuilder().build(normalizedEndpoint(), accessKeyId(), accessKeySecret());
    }

    private String normalizedEndpoint() {
        String endpoint = runtimeConfigService.storageConfig().endpoint();
        if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
            return endpoint;
        }
        return "https://" + endpoint;
    }

    private String bucketName() {
        return runtimeConfigService.storageConfig().bucketName();
    }

    private String buildPublicBucketHost() {
        String endpoint = runtimeConfigService.storageConfig().endpoint();
        String normalizedEndpoint = endpoint
            .replaceFirst("^https?://", "")
            .replaceAll("/+$", "");
        return "https://" + bucketName() + "." + normalizedEndpoint;
    }

    private String accessKeyId() {
        return runtimeConfigService.storageConfig().accessKeyId();
    }

    private String accessKeySecret() {
        return runtimeConfigService.storageConfig().accessKeySecret();
    }

    private String normalizeDir(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.replaceAll("^/+", "").replaceAll("/+$", "");
    }
}
