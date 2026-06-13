import { bindCommunity, createReport, getReportCategories, getRuntimeConfig, loginByWxCode, ReportCategoryResponse } from "../../utils/api";

const COMMUNITY_STORAGE_KEY = "libuke:selectedCommunity";

const defaultCategoryMap: Record<string, string[]> = {
  "公共卫生类": ["楼道卫生", "小区主干道卫生", "垃圾桶满溢", "地下车库卫生"],
  "公共设施类": ["儿童设施损坏", "路灯故障", "门禁损坏", "健身器材损坏"],
  "电梯问题类": ["电梯异响", "电梯停运", "轿厢卫生", "按键故障"],
  "消防安全类": ["通道占用", "消防门损坏", "灭火器缺失", "消防标识缺失"],
  "秩序维护类": ["车辆乱停", "噪音扰民", "门岗缺位", "公共区域堆物"]
};

const defaultCategoryNames = Object.keys(defaultCategoryMap);

Page({
  data: {
    communityName: "",
    showCommunityModal: false,
    inviteCode: "",
    witnessCode: "",
    categoryMap: defaultCategoryMap,
    categoryNames: defaultCategoryNames,
    selectedCategoryIndex: 0,
    selectedSubCategoryIndex: 0,
    subCategoryOptions: defaultCategoryMap[defaultCategoryNames[0]],
    remark: "",
    latitude: 34.2658,
    longitude: 108.9541,
    mapScale: 16,
    locationName: "当前位置",
    locationAddress: "",
    locationWarning: "",
    maxFileCount: 4,
    images: [] as string[]
  },

  onLoad(options?: { inviteCode?: string; scene?: string }) {
    const runtimeConfig = getApp<IAppOption>().globalData.runtimeConfig;
    const title = runtimeConfig?.platform.miniapp_name || runtimeConfig?.platform.platform_name;
    if (title) {
      wx.setNavigationBarTitle({ title });
    }
    this.setData({
      mapScale: runtimeConfig?.map.defaultZoom || 16,
      maxFileCount: getMaxFileCount()
    });
    this.loadRuntimeConfigIfNeeded();
    this.applyInviteCodeFromOptions(options);
    this.restoreCommunityAfterLogin();
    this.loadReportCategories();
    this.loadCurrentLocation();
  },

  async restoreCommunityAfterLogin() {
    try {
      await loginByWxCode();
    } finally {
      this.restoreCommunity();
    }
  },

  async loadRuntimeConfigIfNeeded() {
    if (getApp<IAppOption>().globalData.runtimeConfig) {
      return;
    }
    try {
      const runtimeConfig = await getRuntimeConfig();
      getApp<IAppOption>().globalData.runtimeConfig = runtimeConfig;
      this.setData({
        mapScale: runtimeConfig.map.defaultZoom || 16,
        maxFileCount: getMaxFileCount()
      });
      const title = runtimeConfig.platform.miniapp_name || runtimeConfig.platform.platform_name;
      if (title) {
        wx.setNavigationBarTitle({ title });
      }
    } catch (error) {
      wx.showToast({ title: "配置加载失败，已使用默认配置", icon: "none" });
    }
  },

  applyInviteCodeFromOptions(options?: { inviteCode?: string; scene?: string }) {
    const inviteCode = resolveInviteCode(options);
    if (!inviteCode) {
      return;
    }
    this.setData({
      inviteCode,
      showCommunityModal: true
    });
    this.confirmCommunity();
  },

  async loadReportCategories() {
    try {
      const categories = await getReportCategories();
      const categoryMap = buildCategoryMap(categories);
      const categoryNames = Object.keys(categoryMap);
      if (!categoryNames.length) {
        return;
      }
      this.setData({
        categoryMap,
        categoryNames,
        selectedCategoryIndex: 0,
        selectedSubCategoryIndex: 0,
        subCategoryOptions: categoryMap[categoryNames[0]]
      });
    } catch (error) {
      wx.showToast({ title: "分类加载失败，已使用默认分类", icon: "none" });
    }
  },

  restoreCommunity() {
    const communityName = getApp<IAppOption>().globalData.selectedCommunity;
    if (typeof communityName !== "string" || !communityName) {
      return;
    }

    getApp<IAppOption>().globalData.selectedCommunity = communityName;
    this.setData({
      communityName,
      mapScale: getApp<IAppOption>().globalData.selectedCommunityMapZoom || this.data.mapScale
    });
    this.refreshLocationWarning();
  },

  loadCurrentLocation() {
    wx.getLocation({
      type: "gcj02",
      success: (res) => {
        this.setReportLocation(res.latitude, res.longitude, {
          name: "当前位置",
          address: ""
        });
      },
      fail: () => {
        wx.showToast({ title: "定位获取失败", icon: "none" });
      }
    });
  },

  setReportLocation(
    latitude: number,
    longitude: number,
    options: { name?: string; address?: string } = {}
  ) {
    const locationName = options.name || "取证位置";

    this.setData({
      latitude,
      longitude,
      locationName,
      locationAddress: options.address || "",
      locationWarning: buildLocationWarning(longitude, latitude)
    });
  },

  chooseCustomLocation() {
    wx.chooseLocation({
      latitude: this.data.latitude,
      longitude: this.data.longitude,
      success: (res) => {
        this.setReportLocation(res.latitude, res.longitude, {
          name: res.name || "自定义位置",
          address: res.address || ""
        });
      },
      fail: (error) => {
        if (error.errMsg && error.errMsg.includes("cancel")) {
          return;
        }
        wx.showToast({ title: "位置选择失败", icon: "none" });
      }
    });
  },

  useCurrentLocation() {
    this.loadCurrentLocation();
  },

  goRecords() {
    wx.navigateTo({
      url: "/pages/records/index"
    });
  },

  openCommunityModal() {
    this.setData({ showCommunityModal: true });
  },

  closeCommunityModal() {
    this.setData({ showCommunityModal: false });
  },

  onInviteCodeInput(event: WechatMiniprogram.Input) {
    this.setData({ inviteCode: event.detail.value });
  },

  onWitnessCodeInput(event: WechatMiniprogram.Input) {
    this.setData({ witnessCode: event.detail.value });
  },

  async confirmCommunity() {
    if (!this.data.inviteCode.trim()) {
      wx.showToast({ title: "请输入邀请码", icon: "none" });
      return;
    }

    wx.showLoading({ title: "绑定中", mask: true });
    try {
      const result = await bindCommunity({
        invitationCode: this.data.inviteCode.trim(),
        witnessInfo: this.data.witnessCode.trim()
      });

      getApp<IAppOption>().globalData.selectedCommunity = result.communityName;
      getApp<IAppOption>().globalData.communityId = result.communityId;
      getApp<IAppOption>().globalData.selectedCommunityCenter = result.center;
      getApp<IAppOption>().globalData.selectedCommunityBoundary = result.boundary;
      getApp<IAppOption>().globalData.selectedCommunityMapZoom = result.mapZoom;
      wx.setStorageSync(COMMUNITY_STORAGE_KEY, result.communityName);
      this.setData({
        communityName: result.communityName,
        showCommunityModal: false,
        mapScale: result.mapZoom || this.data.mapScale
      });
      this.refreshLocationWarning();
      wx.showToast({ title: "已绑定", icon: "success" });
    } catch (error) {
      wx.showToast({
        title: error instanceof Error ? error.message : "绑定失败",
        icon: "none"
      });
    } finally {
      wx.hideLoading();
    }
  },

  onCategoryChange(event: WechatMiniprogram.PickerChange) {
    const selectedCategoryIndex = Number(event.detail.value);
    const category = this.data.categoryNames[selectedCategoryIndex];

    this.setData({
      selectedCategoryIndex,
      selectedSubCategoryIndex: 0,
      subCategoryOptions: this.data.categoryMap[category]
    });
  },

  onSubCategoryChange(event: WechatMiniprogram.PickerChange) {
    this.setData({ selectedSubCategoryIndex: Number(event.detail.value) });
  },

  onRemarkInput(event: WechatMiniprogram.Input) {
    this.setData({ remark: event.detail.value });
  },

  previewImage(event: WechatMiniprogram.TouchEvent) {
    const index = Number(event.currentTarget.dataset.index);
    const currentPath = this.data.images[index];

    wx.previewImage({
      current: currentPath,
      urls: this.data.images
    });
  },

  removeImage(event: WechatMiniprogram.TouchEvent<{ index: number }>) {
    const index = Number(event.currentTarget.dataset.index);
    const images = this.data.images.filter((_, imageIndex) => imageIndex !== index);

    this.setData({ images });
  },

  chooseImage() {
    const maxFileCount = this.data.maxFileCount;
    const restCount = maxFileCount - this.data.images.length;
    if (restCount <= 0) {
      wx.showToast({ title: `最多上传 ${maxFileCount} 张`, icon: "none" });
      return;
    }

    wx.chooseMedia({
      count: restCount,
      mediaType: ["image"],
      sizeType: ["original"],
      sourceType: ["camera"],
      success: async (res) => {
        const maxFileSizeMb = getMaxFileSizeMb();
        const oversized = res.tempFiles.find(
          (file) => file.size > maxFileSizeMb * 1024 * 1024
        );
        if (oversized) {
          wx.showToast({ title: `单张图片不能超过 ${maxFileSizeMb}MB`, icon: "none" });
          return;
        }
        const files = res.tempFiles.map((file) => file.tempFilePath);
        this.setData({
          images: [...this.data.images, ...files].slice(0, maxFileCount)
        });
      }
    });
  },

  async submitReport() {
    if (!this.data.communityName) {
      wx.showToast({ title: "请先选择小区", icon: "none" });
      return;
    }

    if (!this.data.images.length) {
      wx.showToast({ title: "请上传图片", icon: "none" });
      return;
    }

    if (this.data.locationWarning) {
      wx.showToast({ title: this.data.locationWarning, icon: "none" });
      return;
    }

    const category = this.data.categoryNames[this.data.selectedCategoryIndex];
    const subCategory = this.data.subCategoryOptions[this.data.selectedSubCategoryIndex];

    wx.showLoading({ title: "提交中", mask: true });
    try {
      await createReport({
        category,
        subCategory,
        longitude: this.data.longitude,
        latitude: this.data.latitude,
        remark: this.data.remark.trim(),
        files: this.data.images
      });

      this.setData({
        remark: "",
        images: []
      });
      wx.showToast({ title: "已提交", icon: "success" });
    } catch (error) {
      wx.showToast({
        title: error instanceof Error ? error.message : "提交失败",
        icon: "none"
      });
    } finally {
      wx.hideLoading();
    }
  },

  refreshLocationWarning() {
    this.setData({
      locationWarning: buildLocationWarning(this.data.longitude, this.data.latitude)
    });
  }
});

function getMaxFileCount() {
  return getApp<IAppOption>().globalData.runtimeConfig?.reportImageUpload.maxFileCount || 4;
}

function getMaxFileSizeMb() {
  return getApp<IAppOption>().globalData.runtimeConfig?.reportImageUpload.maxFileSizeMb || 20;
}

function buildLocationWarning(longitude: number, latitude: number) {
  const boundary = getApp<IAppOption>().globalData.selectedCommunityBoundary;
  if (!boundary || boundary.length < 3) {
    return "";
  }
  return isPointInCommunity(longitude, latitude, boundary)
    ? ""
    : "所选位置不在当前小区范围内，请重新选择位置";
}

function isPointInCommunity(longitude: number, latitude: number, boundary: number[][]) {
  let inside = false;
  const tolerance = 0.0000001;

  for (let index = 0, previousIndex = boundary.length - 1; index < boundary.length; previousIndex = index++) {
    const current = boundary[index];
    const previous = boundary[previousIndex];
    if (!isValidBoundaryPoint(current) || !isValidBoundaryPoint(previous)) {
      return true;
    }

    const [currentLng, currentLat] = current;
    const [previousLng, previousLat] = previous;
    if (isPointOnSegment(longitude, latitude, previousLng, previousLat, currentLng, currentLat, tolerance)) {
      return true;
    }

    const intersect = ((currentLat > latitude) !== (previousLat > latitude))
      && (longitude < ((previousLng - currentLng) * (latitude - currentLat)) / (previousLat - currentLat) + currentLng);
    if (intersect) {
      inside = !inside;
    }
  }

  return inside;
}

function isValidBoundaryPoint(point: number[] | undefined) {
  return Boolean(point && point.length === 2 && Number.isFinite(point[0]) && Number.isFinite(point[1]));
}

function isPointOnSegment(
  longitude: number,
  latitude: number,
  startLng: number,
  startLat: number,
  endLng: number,
  endLat: number,
  tolerance: number
) {
  const cross = (longitude - startLng) * (endLat - startLat) - (latitude - startLat) * (endLng - startLng);
  if (Math.abs(cross) > tolerance) {
    return false;
  }
  return longitude >= Math.min(startLng, endLng) - tolerance
    && longitude <= Math.max(startLng, endLng) + tolerance
    && latitude >= Math.min(startLat, endLat) - tolerance
    && latitude <= Math.max(startLat, endLat) + tolerance;
}

function buildCategoryMap(categories: ReportCategoryResponse[]) {
  return categories.reduce<Record<string, string[]>>((result, category) => {
    const children = category.children || [];
    result[category.name] = children.length ? children.map((child) => child.name) : [category.name];
    return result;
  }, {});
}

function resolveInviteCode(options?: { inviteCode?: string; scene?: string }) {
  if (options?.inviteCode) {
    return decodeURIComponent(options.inviteCode).trim();
  }
  if (!options?.scene) {
    return "";
  }
  const scene = decodeURIComponent(options.scene).trim();
  const pairs = scene.split("&").map((item) => item.split("="));
  const invitePair = pairs.find(([key]) => key === "inviteCode" || key === "code");
  return (invitePair?.[1] || scene).trim();
}
