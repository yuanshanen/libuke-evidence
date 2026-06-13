declare namespace WechatMiniprogram {
  interface BaseEvent<T = Record<string, unknown>> {
    currentTarget: {
      dataset: T;
    };
  }

  interface Input {
    detail: {
      value: string;
    };
  }

  interface PickerChange {
    detail: {
      value: string | number;
    };
  }

  interface TouchEvent<T = Record<string, unknown>> extends BaseEvent<T> {}
}

declare function App<T = Record<string, unknown>>(options: Record<string, unknown> & ThisType<T>): void;

declare function Page(options: Record<string, unknown>): void;

declare function getApp<T = IAppOption>(): T;

declare const wx: {
  request(options: {
    url: string;
    method?: "GET" | "POST" | "PUT" | "DELETE";
    data?: Record<string, unknown> | ArrayBuffer;
    header?: Record<string, string>;
    success(res: {
      data: unknown;
      statusCode: number;
    }): void;
    fail?(error: unknown): void;
  }): void;
  getFileSystemManager(): {
    readFile(options: {
      filePath: string;
      success(res: {
        data: ArrayBuffer;
      }): void;
      fail?(error: unknown): void;
    }): void;
  };
  getStorageSync(key: string): unknown;
  setStorageSync(key: string, data: unknown): void;
  showLoading(options: {
    title: string;
    mask?: boolean;
  }): void;
  hideLoading(): void;
  stopPullDownRefresh(): void;
  showToast(options: {
    title: string;
    icon?: "success" | "error" | "loading" | "none";
    duration?: number;
  }): void;
  chooseMedia(options: {
    count: number;
    mediaType: string[];
    sizeType?: string[];
    sourceType: string[];
    success(res: {
      tempFiles: Array<{
        tempFilePath: string;
      }>;
    }): void;
  }): void;
  compressImage(options: {
    src: string;
    quality?: number;
    success(res: {
      tempFilePath: string;
    }): void;
    fail?(): void;
  }): void;
  previewImage(options: {
    current: string;
    urls: string[];
  }): void;
  getLocation(options: {
    type?: "wgs84" | "gcj02";
    success(res: {
      latitude: number;
      longitude: number;
    }): void;
    fail?(): void;
  }): void;
  navigateTo(options: {
    url: string;
  }): void;
};
