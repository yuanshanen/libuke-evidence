import { getMyReport, getRuntimeConfig, ReportAttachmentResponse } from "../../utils/api";

type AttachmentView = ReportAttachmentResponse & {
  isImage: boolean;
};

type LocationCircle = {
  longitude: number;
  latitude: number;
  radius: number;
  color: string;
  fillColor: string;
  strokeWidth: number;
};

type DetailData = {
  id: string;
  reportNo: string;
  category: string;
  subCategory: string;
  status: string;
  statusText: string;
  statusClass: string;
  remark: string;
  community: string;
  location: string;
  locationAddress: string;
  longitude: number;
  latitude: number;
  hasLocation: boolean;
  mapScale: number;
  locationCircles: LocationCircle[];
  imageCount: number;
  submittedAt: string;
  attachments: AttachmentView[];
  attachmentUrls: string[];
};

Page({
  data: {
    id: "",
    reportNo: "",
    category: "",
    subCategory: "",
    status: "",
    statusText: "",
    statusClass: "",
    remark: "",
    community: "",
    location: "",
    locationAddress: "",
    longitude: 0,
    latitude: 0,
    hasLocation: false,
    mapScale: 16,
    locationCircles: [],
    imageCount: 0,
    submittedAt: "",
    attachments: [] as AttachmentView[],
    attachmentUrls: [] as string[]
  } as DetailData,

  onLoad(options: Record<string, string | undefined>) {
    const imageCount = Number(options.imageCount || 0);
    const cachedReport = getApp<IAppOption>().globalData.currentReport;
    const cachedAttachments = cachedReport?.id === options.id
      ? toAttachmentViews(cachedReport.attachments || [])
      : [];
    const longitude = parseLocationNumber(cachedReport?.longitude, options.longitude);
    const latitude = parseLocationNumber(cachedReport?.latitude, options.latitude);
    const locationAddress = cachedReport?.locationAddress || decodeURIComponent(options.locationAddress || "");

    this.setData({
      id: options.id || "",
      reportNo: cachedReport?.reportNo || decodeURIComponent(options.reportNo || ""),
      category: cachedReport?.category || decodeURIComponent(options.category || ""),
      subCategory: cachedReport?.subCategory || decodeURIComponent(options.subCategory || ""),
      status: cachedReport?.status || decodeURIComponent(options.status || ""),
      statusText: getStatusText(cachedReport?.status || decodeURIComponent(options.status || "")),
      statusClass: getStatusClass(cachedReport?.status || decodeURIComponent(options.status || "")),
      remark: cachedReport?.remark || decodeURIComponent(options.remark || "无"),
      community: cachedReport?.community || decodeURIComponent(options.community || ""),
      location: locationAddress || resolveLocationText(decodeURIComponent(options.location || "")),
      locationAddress,
      longitude,
      latitude,
      hasLocation: hasValidLocation(longitude, latitude),
      mapScale: getApp<IAppOption>().globalData.runtimeConfig?.map.defaultZoom || 16,
      locationCircles: buildLocationCircles(longitude, latitude),
      imageCount: cachedReport?.imageCount || imageCount,
      submittedAt: formatDateTime(cachedReport?.submittedAt || decodeURIComponent(options.submittedAt || "")),
      attachments: cachedAttachments,
      attachmentUrls: cachedAttachments
        .filter((attachment) => attachment.isImage && Boolean(attachment.url))
        .map((attachment) => attachment.url)
    });

    if (options.id) {
      this.loadReportDetail(options.id);
    }
    this.loadRuntimeConfigIfNeeded();
  },

  async loadRuntimeConfigIfNeeded() {
    if (getApp<IAppOption>().globalData.runtimeConfig) {
      return;
    }
    try {
      const runtimeConfig = await getRuntimeConfig();
      getApp<IAppOption>().globalData.runtimeConfig = runtimeConfig;
      this.setData({ mapScale: runtimeConfig.map.defaultZoom || 16 });
    } catch (error) {
      wx.showToast({ title: "地图配置加载失败，已使用默认缩放", icon: "none" });
    }
  },

  async loadReportDetail(reportId: string) {
    wx.showLoading({ title: "加载中", mask: true });
    try {
      const report = await getMyReport(reportId);
      const attachments = toAttachmentViews(report.attachments || []);

      this.setData({
        reportNo: report.reportNo,
        category: report.category,
        subCategory: report.subCategory,
        status: report.status || "",
        statusText: getStatusText(report.status),
        statusClass: getStatusClass(report.status),
        remark: report.remark || "无",
        community: report.communityName,
        location: report.locationAddress || "未记录位置",
        locationAddress: report.locationAddress || "",
        longitude: report.longitude || 0,
        latitude: report.latitude || 0,
        hasLocation: hasValidLocation(report.longitude, report.latitude),
        locationCircles: buildLocationCircles(report.longitude, report.latitude),
        imageCount: report.imageCount,
        submittedAt: formatDateTime(report.submittedAt),
        attachments,
        attachmentUrls: attachments
          .filter((attachment) => attachment.isImage && Boolean(attachment.url))
          .map((attachment) => attachment.url)
      });
    } catch (error) {
      wx.showToast({
        title: error instanceof Error ? error.message : "加载失败",
        icon: "none"
      });
    } finally {
      wx.hideLoading();
    }
  },

  previewAttachment(event: WechatMiniprogram.TouchEvent<{ index: number }>) {
    const index = Number(event.currentTarget.dataset.index);
    const attachment = this.data.attachments[index];

    if (!attachment || !attachment.isImage) {
      return;
    }

    wx.previewImage({
      current: attachment.url,
      urls: this.data.attachmentUrls
    });
  }
});

function formatDateTime(value: string) {
  if (!value) {
    return "";
  }

  return value
    .replace("T", " ")
    .replace(/\.\d+$/, "")
    .slice(0, 16);
}

function parseLocationNumber(cachedValue?: number, optionValue?: string) {
  if (typeof cachedValue === "number") {
    return cachedValue;
  }

  const parsedValue = Number(optionValue || 0);
  return Number.isFinite(parsedValue) ? parsedValue : 0;
}

function hasValidLocation(longitude?: number, latitude?: number) {
  return Boolean(longitude && latitude);
}

function resolveLocationText(value: string) {
  if (!value || value.includes(",")) {
    return "未记录位置";
  }

  return value;
}

function getStatusText(status?: string) {
  const statusTextMap: Record<string, string> = {
    pending: "待处理",
    assigned: "已派单",
    processing: "处理中",
    resolved: "已处理",
    closed: "已关闭",
    invalid: "无效",
    duplicate: "重复"
  };

  return statusTextMap[status || ""] || "待处理";
}

function getStatusClass(status?: string) {
  const normalizedStatus = status || "pending";
  return `status-badge ${normalizedStatus}`;
}

function buildLocationCircles(longitude?: number, latitude?: number) {
  if (!hasValidLocation(longitude, latitude)) {
    return [];
  }

  return [{
    longitude: Number(longitude),
    latitude: Number(latitude),
    radius: 22,
    color: "#168b62AA",
    fillColor: "#168b6233",
    strokeWidth: 2
  }];
}

function toAttachmentViews(attachments: ReportAttachmentResponse[]): AttachmentView[] {
  return attachments.map((attachment) => ({
    ...attachment,
    isImage: attachment.type === "image" && Boolean(attachment.url)
  }));
}
