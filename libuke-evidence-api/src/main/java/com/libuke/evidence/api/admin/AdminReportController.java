package com.libuke.evidence.api.admin;

import com.libuke.evidence.api.admin.dto.AdminReportEventResponse;
import com.libuke.evidence.api.admin.dto.AdminReportExportRequest;
import com.libuke.evidence.api.admin.dto.AdminReportMapPointResponse;
import com.libuke.evidence.api.admin.dto.AdminReportResponse;
import com.libuke.evidence.api.admin.dto.AdminReportStatusRequest;
import com.libuke.evidence.api.dto.PageResponse;
import com.libuke.evidence.common.ApiResponse;
import com.libuke.evidence.domain.service.AdminService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @author libuke
 * @date 2026-06-06 00:00
 * @desc 后台问题记录管理 提供问题查询、地图点位、处置状态和事件时间线能力
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/reports")
public class AdminReportController {

    private final AdminService adminService;

    /**
     * 分页查询问题记录
     *
     * @param keyword 关键词
     * @param communityId 小区ID
     * @param category 问题分类
     * @param status 处置状态
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @return 问题分页数据
     */
    @GetMapping
    public ApiResponse<PageResponse<AdminReportResponse>> pageReports(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long communityId,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String status,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(required = false) LocalDate startDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(required = false) LocalDate endDate,
        @RequestParam(defaultValue = "1") long pageNo,
        @RequestParam(defaultValue = "10") long pageSize
    ) {
        return ApiResponse.ok(adminService.pageReports(keyword, communityId, category, status, startDate, endDate, pageNo, pageSize));
    }

    /**
     * 导出问题记录台账
     *
     * @param request 导出条件
     * @param response 响应流
     * @throws IOException Excel 写出失败
     */
    @GetMapping("/export")
    public void exportReports(
        @ModelAttribute AdminReportExportRequest request,
        HttpServletResponse response
    ) throws IOException {
        String fileName = "问题记录_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
        adminService.exportReports(request, response.getOutputStream());
    }

    /**
     * 查询问题地图点位
     *
     * @param communityId 小区ID
     * @param status 记录状态，默认查询待处理
     * @param limit 最大返回数量
     * @return 地图点位列表
     */
    @GetMapping("/map-points")
    public ApiResponse<List<AdminReportMapPointResponse>> listMapPoints(
        @RequestParam Long communityId,
        @RequestParam(defaultValue = "pending") String status,
        @RequestParam(defaultValue = "200") long limit
    ) {
        return ApiResponse.ok(adminService.listReportMapPoints(communityId, status, limit));
    }

    /**
     * 订阅当前小区问题上报事件
     *
     * @param communityId 小区ID
     * @return SSE 连接
     */
    @GetMapping(value = "/stream", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter stream(@RequestParam Long communityId) {
        return adminService.subscribeReportStream(communityId);
    }

    /**
     * 查询问题处置时间线
     *
     * @param reportId 记录ID
     * @return 事件列表
     */
    @GetMapping("/{reportId}/events")
    public ApiResponse<List<AdminReportEventResponse>> listEvents(@PathVariable Long reportId) {
        return ApiResponse.ok(adminService.listReportEvents(reportId));
    }

    /**
     * 查询问题详情
     *
     * @param reportId 记录ID
     * @return 问题详情
     */
    @GetMapping("/{reportId}")
    public ApiResponse<AdminReportResponse> getReport(@PathVariable Long reportId) {
        return ApiResponse.ok(adminService.getReport(reportId));
    }

    /**
     * 更新问题处置状态
     *
     * @param reportId 记录ID
     * @param request 处置信息
     * @return 更新后的问题详情
     */
    @PatchMapping("/{reportId}/status")
    public ApiResponse<AdminReportResponse> updateStatus(
        @PathVariable Long reportId,
        @Valid @RequestBody AdminReportStatusRequest request
    ) {
        return ApiResponse.ok(adminService.updateReportStatus(reportId, request));
    }

    /**
     * 删除问题记录
     *
     * @param reportId 记录ID
     * @return 空响应
     */
    @DeleteMapping("/{reportId}")
    public ApiResponse<Void> deleteReport(@PathVariable Long reportId) {
        adminService.deleteReport(reportId);
        return ApiResponse.ok(null);
    }
}
