import { getRuntimeConfig, loginByWxCode } from "./utils/api";

App<IAppOption>({
  onLaunch() {
    getRuntimeConfig()
      .then((config) => {
        this.globalData.runtimeConfig = config;
        const title = config.platform.miniapp_name || config.platform.platform_name;
        if (title) {
          wx.setNavigationBarTitle({ title });
        }
      })
      .catch(() => undefined);
    loginByWxCode().catch(() => undefined);
  },

  globalData: {
    apiBaseUrl: "http://127.0.0.1:8080",
    openid: "",
    userId: undefined,
    communityId: undefined,
    selectedCommunity: "",
    selectedCommunityCenter: undefined,
    selectedCommunityBoundary: undefined,
    selectedCommunityMapZoom: undefined,
    currentReport: undefined
  }
});
