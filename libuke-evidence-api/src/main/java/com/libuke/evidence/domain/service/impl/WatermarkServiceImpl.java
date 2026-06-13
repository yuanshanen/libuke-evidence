package com.libuke.evidence.domain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.libuke.evidence.api.admin.dto.AdminWatermarkTaskResponse;
import com.libuke.evidence.api.admin.dto.AdminWatermarkTemplateRequest;
import com.libuke.evidence.api.admin.dto.AdminWatermarkTemplateResponse;
import com.libuke.evidence.api.dto.PageResponse;
import com.libuke.evidence.common.BusinessException;
import com.libuke.evidence.domain.entity.Community;
import com.libuke.evidence.domain.entity.Report;
import com.libuke.evidence.domain.entity.ReportImage;
import com.libuke.evidence.domain.entity.WatermarkTask;
import com.libuke.evidence.domain.entity.WatermarkTemplate;
import com.libuke.evidence.domain.mapper.CommunityMapper;
import com.libuke.evidence.domain.mapper.ReportImageMapper;
import com.libuke.evidence.domain.mapper.ReportMapper;
import com.libuke.evidence.domain.mapper.WatermarkTaskMapper;
import com.libuke.evidence.domain.mapper.WatermarkTemplateMapper;
import com.libuke.evidence.domain.service.WatermarkService;
import com.libuke.evidence.integration.oss.OssUploadService;
import com.libuke.evidence.integration.oss.WatermarkRenderOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WatermarkServiceImpl implements WatermarkService {

    private static final String DEFAULT_TEMPLATE = """
        {{communityName}}｜{{category}} / {{subCategory}}
        位置：{{locationAddress}}
        上报时间：{{submittedAt}}
        编号：{{reportNo}}
        """;
    private static final Charset GBK = Charset.forName("GBK");

    private final WatermarkTaskMapper watermarkTaskMapper;
    private final WatermarkTemplateMapper watermarkTemplateMapper;
    private final ReportImageMapper reportImageMapper;
    private final ReportMapper reportMapper;
    private final CommunityMapper communityMapper;
    private final OssUploadService ossUploadService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTask(Long imageId) {
        ReportImage image = reportImageMapper.selectById(imageId);
        if (image == null) {
            throw new BusinessException("附件不存在");
        }
        WatermarkTemplate template = loadEnabledTemplate();
        WatermarkTask task = new WatermarkTask();
        task.setReportId(image.getReportId());
        task.setImageId(image.getId());
        task.setTemplateId(template.getId());
        task.setStatus("pending");
        task.setRetryCount(0);
        watermarkTaskMapper.insert(task);
    }

    @Async
    @Override
    public void processReportAsync(Long reportId) {
        List<WatermarkTask> tasks = watermarkTaskMapper.selectList(
            new LambdaQueryWrapper<WatermarkTask>()
                .eq(WatermarkTask::getReportId, reportId)
                .in(WatermarkTask::getStatus, List.of("pending", "failed"))
                .orderByAsc(WatermarkTask::getCreatedAt)
        );
        for (WatermarkTask task : tasks) {
            processTask(task.getId());
        }
    }

    @Override
    public void retryTask(Long taskId) {
        WatermarkTask task = watermarkTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("水印任务不存在");
        }
        task.setStatus("pending");
        task.setErrorMessage(null);
        watermarkTaskMapper.updateById(task);
        processTask(taskId);
    }

    @Override
    public PageResponse<AdminWatermarkTaskResponse> pageTasks(String status, long pageNo, long pageSize) {
        Page<WatermarkTask> page = watermarkTaskMapper.selectPage(
            Page.of(pageNo, Math.min(Math.max(pageSize, 1), 100)),
            new LambdaQueryWrapper<WatermarkTask>()
                .eq(StringUtils.hasText(status), WatermarkTask::getStatus, status)
                .orderByDesc(WatermarkTask::getCreatedAt)
        );
        Map<Long, Report> reportMap = loadReportMap(page.getRecords());
        Map<Long, WatermarkTemplate> templateMap = loadTemplateMap(page.getRecords());
        return PageResponse.<AdminWatermarkTaskResponse>builder()
            .total(page.getTotal())
            .pageNo(page.getCurrent())
            .pageSize(page.getSize())
            .records(page.getRecords().stream()
                .map(task -> toTaskResponse(task, reportMap.get(task.getReportId()), templateMap.get(task.getTemplateId())))
                .toList())
            .build();
    }

    @Override
    public List<AdminWatermarkTemplateResponse> listTemplates() {
        return watermarkTemplateMapper.selectList(
                new LambdaQueryWrapper<WatermarkTemplate>().orderByDesc(WatermarkTemplate::getEnabled).orderByDesc(WatermarkTemplate::getUpdatedAt)
            )
            .stream()
            .map(this::toTemplateResponse)
            .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminWatermarkTemplateResponse createTemplate(AdminWatermarkTemplateRequest request) {
        WatermarkTemplate template = new WatermarkTemplate();
        fillTemplate(template, request);
        watermarkTemplateMapper.insert(template);
        disableOtherTemplatesIfNeeded(template);
        return toTemplateResponse(watermarkTemplateMapper.selectById(template.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminWatermarkTemplateResponse updateTemplate(Long templateId, AdminWatermarkTemplateRequest request) {
        WatermarkTemplate template = watermarkTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException("水印模板不存在");
        }
        fillTemplate(template, request);
        watermarkTemplateMapper.updateById(template);
        disableOtherTemplatesIfNeeded(template);
        return toTemplateResponse(watermarkTemplateMapper.selectById(templateId));
    }

    private void processTask(Long taskId) {
        WatermarkTask task = watermarkTaskMapper.selectById(taskId);
        if (task == null || "processing".equals(task.getStatus())) {
            return;
        }
        ReportImage image = reportImageMapper.selectById(task.getImageId());
        Report report = task.getReportId() == null ? null : reportMapper.selectById(task.getReportId());
        WatermarkTemplate template = task.getTemplateId() == null ? loadEnabledTemplate() : watermarkTemplateMapper.selectById(task.getTemplateId());
        if (image == null || report == null || template == null) {
            markFailed(task, "水印任务关联数据不存在");
            return;
        }

        task.setStatus("processing");
        task.setRetryCount(task.getRetryCount() == null ? 1 : task.getRetryCount() + 1);
        task.setErrorMessage(null);
        watermarkTaskMapper.updateById(task);
        image.setProcessStatus("processing");
        reportImageMapper.updateById(image);

        try {
            String watermarkedObjectKey = ossUploadService.generateWatermarkedReportImage(
                image.getOriginalObjectKey(),
                buildWatermarkLines(template, report),
                toRenderOptions(template)
            );
            image.setWatermarkedObjectKey(watermarkedObjectKey);
            image.setProcessStatus("success");
            reportImageMapper.updateById(image);
            task.setStatus("success");
            watermarkTaskMapper.updateById(task);
        } catch (RuntimeException exception) {
            image.setProcessStatus("failed");
            reportImageMapper.updateById(image);
            markFailed(task, exception.getMessage());
            log.warn("[watermark] task failed, taskId={}, imageId={}", taskId, image.getId(), exception);
        }
    }

    private WatermarkTemplate loadEnabledTemplate() {
        WatermarkTemplate template = watermarkTemplateMapper.selectOne(
            new LambdaQueryWrapper<WatermarkTemplate>()
                .eq(WatermarkTemplate::getEnabled, true)
                .orderByDesc(WatermarkTemplate::getUpdatedAt)
                .last("limit 1")
        );
        if (template != null) {
            return template;
        }
        template = new WatermarkTemplate();
        template.setName("默认水印模板");
        template.setEnabled(true);
        template.setPosition("bottom");
        template.setOpacity(new BigDecimal("1.00"));
        template.setBackgroundOpacity(new BigDecimal("0.58"));
        template.setTextColor("#FFFFFF");
        template.setBackgroundColor("#000000");
        template.setContentTemplate(DEFAULT_TEMPLATE.trim());
        template.setSystemTemplate(true);
        watermarkTemplateMapper.insert(template);
        return template;
    }

    private List<String> buildWatermarkLines(WatermarkTemplate template, Report report) {
        Community community = communityMapper.selectById(report.getCommunityId());
        String content = repairMojibake(template.getContentTemplate());
        Map<String, String> values = Map.of(
            "communityName", repairMojibake(community == null ? "" : community.getName()),
            "category", repairMojibake(nullToBlank(report.getCategory())),
            "subCategory", repairMojibake(nullToBlank(report.getSubCategory())),
            "locationAddress", repairMojibake(StringUtils.hasText(report.getLocationAddress()) ? report.getLocationAddress() : "未知地址"),
            "longitude", report.getLongitude() == null ? "" : report.getLongitude().toPlainString(),
            "latitude", report.getLatitude() == null ? "" : report.getLatitude().toPlainString(),
            "submittedAt", report.getSubmittedAt() == null ? "" : report.getSubmittedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
            "reportNo", nullToBlank(report.getReportNo())
        );
        for (Map.Entry<String, String> entry : values.entrySet()) {
            content = content.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return content.lines().filter(StringUtils::hasText).toList();
    }

    private WatermarkRenderOptions toRenderOptions(WatermarkTemplate template) {
        return new WatermarkRenderOptions(
            template.getPosition(),
            template.getOpacity(),
            template.getBackgroundOpacity(),
            template.getFontSize(),
            template.getTextColor(),
            template.getBackgroundColor()
        );
    }

    private void fillTemplate(WatermarkTemplate template, AdminWatermarkTemplateRequest request) {
        template.setName(request.getName().trim());
        template.setEnabled(request.getEnabled() == null || request.getEnabled());
        template.setPosition(StringUtils.hasText(request.getPosition()) ? request.getPosition() : "bottom");
        template.setOpacity(request.getOpacity() == null ? new BigDecimal("1.00") : request.getOpacity());
        template.setBackgroundOpacity(request.getBackgroundOpacity() == null ? new BigDecimal("0.58") : request.getBackgroundOpacity());
        template.setFontSize(request.getFontSize());
        template.setTextColor(StringUtils.hasText(request.getTextColor()) ? request.getTextColor() : "#FFFFFF");
        template.setBackgroundColor(StringUtils.hasText(request.getBackgroundColor()) ? request.getBackgroundColor() : "#000000");
        template.setContentTemplate(request.getContentTemplate().trim());
        if (template.getSystemTemplate() == null) {
            template.setSystemTemplate(false);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long templateId) {
        WatermarkTemplate template = watermarkTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException("水印模板不存在");
        }
        if (Boolean.TRUE.equals(template.getSystemTemplate())) {
            throw new BusinessException("系统模板不允许删除");
        }
        if (Boolean.TRUE.equals(template.getEnabled())) {
            enableSystemTemplate();
        }
        watermarkTemplateMapper.deleteById(templateId);
    }

    private void enableSystemTemplate() {
        WatermarkTemplate systemTemplate = watermarkTemplateMapper.selectOne(
            new LambdaQueryWrapper<WatermarkTemplate>()
                .eq(WatermarkTemplate::getSystemTemplate, true)
                .last("limit 1")
        );
        if (systemTemplate != null) {
            systemTemplate.setEnabled(true);
            watermarkTemplateMapper.updateById(systemTemplate);
        }
    }

    private void disableOtherTemplatesIfNeeded(WatermarkTemplate template) {
        if (!Boolean.TRUE.equals(template.getEnabled())) {
            return;
        }
        List<WatermarkTemplate> enabledTemplates = watermarkTemplateMapper.selectList(
            new LambdaQueryWrapper<WatermarkTemplate>()
                .eq(WatermarkTemplate::getEnabled, true)
                .ne(WatermarkTemplate::getId, template.getId())
        );
        for (WatermarkTemplate item : enabledTemplates) {
            item.setEnabled(false);
            watermarkTemplateMapper.updateById(item);
        }
    }

    private void markFailed(WatermarkTask task, String errorMessage) {
        task.setStatus("failed");
        task.setErrorMessage(StringUtils.hasText(errorMessage) ? errorMessage : "水印生成失败");
        watermarkTaskMapper.updateById(task);
    }

    private Map<Long, Report> loadReportMap(List<WatermarkTask> tasks) {
        List<Long> ids = tasks.stream().map(WatermarkTask::getReportId).filter(id -> id != null).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return reportMapper.selectBatchIds(ids).stream()
            .collect(Collectors.toMap(Report::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, WatermarkTemplate> loadTemplateMap(List<WatermarkTask> tasks) {
        List<Long> ids = tasks.stream().map(WatermarkTask::getTemplateId).filter(id -> id != null).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return watermarkTemplateMapper.selectBatchIds(ids).stream()
            .collect(Collectors.toMap(WatermarkTemplate::getId, Function.identity(), (left, right) -> left));
    }

    private AdminWatermarkTaskResponse toTaskResponse(WatermarkTask task, Report report, WatermarkTemplate template) {
        return AdminWatermarkTaskResponse.builder()
            .id(task.getId())
            .reportId(task.getReportId())
            .reportNo(report == null ? null : report.getReportNo())
            .imageId(task.getImageId())
            .templateId(task.getTemplateId())
            .templateName(template == null ? null : template.getName())
            .status(task.getStatus())
            .retryCount(task.getRetryCount())
            .errorMessage(task.getErrorMessage())
            .createdAt(task.getCreatedAt())
            .updatedAt(task.getUpdatedAt())
            .build();
    }

    private AdminWatermarkTemplateResponse toTemplateResponse(WatermarkTemplate template) {
        return AdminWatermarkTemplateResponse.builder()
            .id(template.getId())
            .name(template.getName())
            .enabled(template.getEnabled())
            .position(template.getPosition())
            .opacity(template.getOpacity())
            .backgroundOpacity(template.getBackgroundOpacity())
            .fontSize(template.getFontSize())
            .textColor(template.getTextColor())
            .backgroundColor(template.getBackgroundColor())
            .contentTemplate(template.getContentTemplate())
            .systemTemplate(template.getSystemTemplate())
            .createdAt(template.getCreatedAt())
            .updatedAt(template.getUpdatedAt())
            .build();
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private String repairMojibake(String value) {
        if (!StringUtils.hasText(value) || !looksLikeChineseMojibake(value)) {
            return value;
        }
        try {
            String repaired = new String(value.getBytes(GBK), StandardCharsets.UTF_8);
            return StringUtils.hasText(repaired) ? repaired : value;
        } catch (RuntimeException exception) {
            return value;
        }
    }

    private boolean looksLikeChineseMojibake(String value) {
        return value.contains("鍐")
            || value.contains("鍙")
            || value.contains("鍦")
            || value.contains("涓")
            || value.contains("姘")
            || value.contains("璇")
            || value.contains("榛")
            || value.contains("鐢")
            || value.contains("绫")
            || value.contains("锛");
    }
}
