import { getReportCategories, pageMyReports, ReportAttachmentResponse, ReportRecordResponse } from "../../utils/api";

type ReportRecord = {
  id: string;
  reportNo: string;
  category: string;
  subCategory: string;
  remark: string;
  status?: string;
  statusText: string;
  statusClass: string;
  community: string;
  location: string;
  longitude?: number;
  latitude?: number;
  locationAddress?: string;
  coverImageUrl: string;
  imageCount: number;
  imageBadgeText: string;
  submittedAt: string;
  thumbClass: string;
  attachments: ReportAttachmentResponse[];
};

const filterOptions = [
  "全部问题大类",
  "公共卫生类",
  "公共设施类",
  "电梯问题类",
  "消防安全类",
  "秩序维护类"
];

Page({
  data: {
    filterOptions,
    selectedFilterIndex: 0,
    visibleRecords: [] as ReportRecord[]
  },

  onLoad() {
    this.loadFilterOptions();
    this.loadRecords();
  },

  async loadFilterOptions() {
    try {
      const categories = await getReportCategories();
      const names = categories.map((category) => category.name).filter(Boolean);
      this.setData({
        filterOptions: ["全部问题大类", ...names]
      });
    } catch (error) {
      wx.showToast({ title: "分类加载失败，已使用默认分类", icon: "none" });
    }
  },

  onPullDownRefresh() {
    this.loadRecords(true);
  },

  onFilterChange(event: WechatMiniprogram.PickerChange) {
    const selectedFilterIndex = Number(event.detail.value);

    this.setData({ selectedFilterIndex });
    this.loadRecords();
  },

  async loadRecords(isRefresh = false) {
    const selectedFilter = this.data.filterOptions[this.data.selectedFilterIndex];
    const category = selectedFilter === "全部问题大类" ? "" : selectedFilter;

    if (!isRefresh) {
      wx.showLoading({ title: "加载中", mask: true });
    }
    try {
      const page = await pageMyReports({
        category,
        pageNo: 1,
        pageSize: 20
      });

      this.setData({
        visibleRecords: page.records.map(toRecord)
      });
    } catch (error) {
      wx.showToast({
        title: error instanceof Error ? error.message : "加载失败",
        icon: "none"
      });
    } finally {
      if (isRefresh) {
        wx.stopPullDownRefresh();
      } else {
        wx.hideLoading();
      }
    }
  },

  openRecordDetail(event: WechatMiniprogram.TouchEvent<{ index: number }>) {
    const index = Number(event.currentTarget.dataset.index);
    const record = this.data.visibleRecords[index];
    if (!record) {
      return;
    }

    getApp<IAppOption>().globalData.currentReport = record;

    const query = [
      ["id", String(record.id)],
      ["reportNo", record.reportNo],
      ["category", record.category],
      ["subCategory", record.subCategory],
      ["remark", record.remark],
      ["status", record.status || ""],
      ["community", record.community],
      ["location", record.location],
      ["longitude", record.longitude === undefined ? "" : String(record.longitude)],
      ["latitude", record.latitude === undefined ? "" : String(record.latitude)],
      ["locationAddress", record.locationAddress || ""],
      ["imageCount", String(record.imageCount)],
      ["submittedAt", record.submittedAt]
    ]
      .map(([key, value]) => `${key}=${encodeURIComponent(value)}`)
      .join("&");

    wx.navigateTo({
      url: `/pages/record-detail/index?${query}`
    });
  }
});

function toRecord(record: ReportRecordResponse): ReportRecord {
  return {
    id: record.id,
    reportNo: record.reportNo,
    category: record.category,
    subCategory: record.subCategory,
    remark: record.remark || "无",
    status: record.status,
    statusText: getStatusText(record.status),
    statusClass: getStatusClass(record.status),
    community: record.communityName,
    location: record.locationAddress || "未记录位置",
    longitude: record.longitude,
    latitude: record.latitude,
    locationAddress: record.locationAddress,
    coverImageUrl: record.firstImageUrl || "",
    imageCount: record.imageCount,
    imageBadgeText: record.imageCount > 0 ? `${record.imageCount}个` : "",
    submittedAt: record.submittedAt,
    thumbClass: record.category === "公共设施类" ? "facility" : "",
    attachments: record.attachments || []
  };
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
