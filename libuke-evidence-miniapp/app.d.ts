interface IAppOption {
  globalData: {
    apiBaseUrl: string;
    openid: string;
    userId?: number;
    communityId?: number;
    selectedCommunity: string;
    selectedCommunityCenter?: number[];
    selectedCommunityBoundary?: number[][];
    selectedCommunityMapZoom?: number;
    runtimeConfig?: {
      platform: Record<string, string>;
      map: {
        jsApiKey?: string;
        jsApiSecurityKey?: string;
        defaultZoom: number;
        defaultLongitude?: number;
        defaultLatitude?: number;
      };
      reportImageUpload: {
        maxFileCount: number;
        maxFileSizeMb: number;
        allowedMimeTypes: string;
        compressEnabled: boolean;
      };
    };
    currentReport?: {
      id: string;
      reportNo: string;
      category: string;
      subCategory: string;
      remark: string;
      status?: string;
      community: string;
      location: string;
      longitude?: number;
      latitude?: number;
      locationAddress?: string;
      imageCount: number;
      submittedAt: string;
      attachments?: Array<{
        id: string;
        objectKey: string;
        url: string;
        type: "image" | "video";
        sortOrder: number;
      }>;
    };
  };
}
