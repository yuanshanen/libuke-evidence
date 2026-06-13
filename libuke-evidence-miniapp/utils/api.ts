type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
};

type CommunityBindingResponse = {
  userId: number;
  communityId: number;
  communityName: string;
  authStatus: string;
  center?: number[];
  boundary?: number[][];
  mapZoom?: number;
};

type MiniLoginResponse = {
  openid: string;
  userId: number;
  communityId?: number;
  communityName?: string;
  authStatus: string;
  center?: number[];
  boundary?: number[][];
  mapZoom?: number;
};

const COMMUNITY_STORAGE_KEY = "libuke:selectedCommunity";

export type RuntimeConfigResponse = {
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

export type ReportRecordResponse = {
  id: string;
  reportNo: string;
  communityName: string;
  category: string;
  subCategory: string;
  remark?: string;
  status?: string;
  longitude?: number;
  latitude?: number;
  locationAddress?: string;
  firstImageObjectKey?: string;
  firstImageUrl?: string;
  imageCount: number;
  attachments?: ReportAttachmentResponse[];
  submittedAt: string;
};

export type ReportAttachmentResponse = {
  clientUploadedAt?: string;
  id: string;
  imageHeight?: number;
  imageWidth?: number;
  objectKey: string;
  originalFileSize?: number;
  originalFileName?: string;
  originalMimeType?: string;
  serverReceivedAt?: string;
  url: string;
  type: "image" | "video";
  sortOrder: number;
};

export type ReportCategoryResponse = {
  id: string;
  parentId?: string;
  name: string;
  code?: string;
  sortOrder: number;
  enabled: boolean;
  children?: ReportCategoryResponse[];
};

type ReportImageUploadResponse = {
  objectKey: string;
  url: string;
};

type UploadedReportImage = ReportImageUploadResponse & {
  clientUploadedAt: string;
  originalFileName: string;
};

type DirectUploadPolicyResponse = {
  host: string;
  objectKey: string;
  accessKeyId: string;
  policy: string;
  signature: string;
  successActionStatus: string;
  expireAt: number;
};

type PageResponse<T> = {
  total: number;
  pageNo: number;
  pageSize: number;
  records: T[];
};

let loginPromise: Promise<string> | undefined;

function getBaseUrl() {
  return getApp<IAppOption>().globalData.apiBaseUrl;
}

function unwrapResponse<T>(response: ApiResponse<T>) {
  if (response.code !== 0) {
    throw new Error(response.message || "请求失败");
  }

  return response.data;
}

export function request<T>(options: {
  url: string;
  method?: "GET" | "POST";
  data?: Record<string, unknown>;
}): Promise<T> {
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${getBaseUrl()}/api${options.url}`,
      method: options.method || "GET",
      data: options.data,
      header: {
        'content-type': 'application/json'
      },
      success: (res) => {
        try {
          resolve(unwrapResponse<T>(res.data as ApiResponse<T>));
        } catch (error) {
          reject(error);
        }
      },
      fail: reject
    });
  });
}

export function getRuntimeConfig() {
  return request<RuntimeConfigResponse>({
    url: "/open/v1/config"
  });
}

export function getReportCategories() {
  return request<ReportCategoryResponse[]>({
    url: "/miniapp/v1/report-categories"
  });
}

export async function loginByWxCode() {
  const openid = await ensureOpenid(true);
  return openid;
}

export async function bindCommunity(params: {
  invitationCode: string;
  witnessInfo?: string;
}) {
  const openid = await ensureOpenid();
  return request<CommunityBindingResponse>({
    url: "/miniapp/v1/communities/bind",
    method: "POST",
    data: {
      openid,
      invitationCode: params.invitationCode,
      witnessInfo: params.witnessInfo || ""
    }
  });
}

export async function createReport(params: {
  category: string;
  subCategory: string;
  longitude: number;
  latitude: number;
  remark: string;
  files: string[];
}): Promise<ReportRecordResponse> {
  const uploadedImages = await Promise.all(
    params.files.map((filePath, index) => uploadReportImageDirectlyToOss(filePath, index))
  );
  const openid = await ensureOpenid();
  return request<ReportRecordResponse>({
    url: "/miniapp/v1/reports/json",
    method: "POST",
    data: {
      openid,
      category: params.category,
      subCategory: params.subCategory,
      longitude: params.longitude,
      latitude: params.latitude,
      remark: params.remark,
      imageObjectKeys: uploadedImages.map((image) => image.objectKey),
      imageEvidenceList: uploadedImages.map((image) => ({
        objectKey: image.objectKey,
        clientUploadedAt: image.clientUploadedAt,
        originalFileName: image.originalFileName
      }))
    }
  });
}

export async function createReportMultipart(params: {
  category: string;
  subCategory: string;
  longitude: number;
  latitude: number;
  remark: string;
  files: string[];
}): Promise<ReportRecordResponse> {
  const openid = await ensureOpenid();
  return postMultipart<ReportRecordResponse>({
    url: "/miniapp/v1/reports",
    fields: {
      openid,
      category: params.category,
      subCategory: params.subCategory,
      longitude: String(params.longitude),
      latitude: String(params.latitude),
      remark: params.remark
    },
    files: params.files.map((filePath, index) => ({
      fieldName: "files",
      filePath,
      fileName: buildUploadFileName(filePath, index),
      contentType: inferImageContentType(filePath)
    }))
  });
}

export async function pageMyReports(params: {
  category?: string;
  pageNo?: number;
  pageSize?: number;
}) {
  const openid = await ensureOpenid();
  return request<PageResponse<ReportRecordResponse>>({
    url: "/miniapp/v1/reports",
    data: {
      openid,
      category: params.category || "",
      pageNo: params.pageNo || 1,
      pageSize: params.pageSize || 20
    }
  });
}

export async function getMyReport(reportId: string | number) {
  const openid = await ensureOpenid();
  return request<ReportRecordResponse>({
    url: `/miniapp/v1/reports/${reportId}`,
    data: {
      openid
    }
  });
}

function ensureOpenid(forceRefresh = false): Promise<string> {
  const app = getApp<IAppOption>();
  if (!forceRefresh && app.globalData.openid) {
    return Promise.resolve(app.globalData.openid);
  }
  if (!forceRefresh && loginPromise) {
    return loginPromise;
  }
  loginPromise = new Promise((resolve, reject) => {
    wx.login({
      success: async (loginRes) => {
        try {
          const response = await request<MiniLoginResponse>({
            url: "/miniapp/v1/auth/login",
            method: "POST",
            data: { code: loginRes.code }
          });
          app.globalData.openid = response.openid;
          app.globalData.userId = response.userId;
          app.globalData.communityId = response.communityId;
          if (response.communityName) {
            app.globalData.selectedCommunity = response.communityName;
            app.globalData.selectedCommunityCenter = response.center;
            app.globalData.selectedCommunityBoundary = response.boundary;
            app.globalData.selectedCommunityMapZoom = response.mapZoom;
            wx.setStorageSync(COMMUNITY_STORAGE_KEY, response.communityName);
          } else {
            app.globalData.selectedCommunity = "";
            app.globalData.selectedCommunityCenter = undefined;
            app.globalData.selectedCommunityBoundary = undefined;
            app.globalData.selectedCommunityMapZoom = undefined;
            wx.removeStorageSync(COMMUNITY_STORAGE_KEY);
          }
          resolve(response.openid);
        } catch (error) {
          reject(error);
        }
      },
      fail: reject
    });
  }).catch((error) => {
    loginPromise = undefined;
    throw error;
  });
  return loginPromise;
}

function uploadReportImageDirectlyToOss(
  filePath: string,
  index: number
): Promise<UploadedReportImage> {
  const originalFileName = buildUploadFileName(filePath, index);
  return ensureOpenid()
    .then((openid) => createDirectUploadPolicy(openid, originalFileName))
    .then((policy) => new Promise<UploadedReportImage>((resolve, reject) => {
      wx.uploadFile({
        url: normalizeOssHost(policy.host),
        filePath,
        name: "file",
        formData: {
          key: policy.objectKey,
          policy: policy.policy,
          OSSAccessKeyId: policy.accessKeyId,
          Signature: policy.signature,
          success_action_status: policy.successActionStatus || "200"
        },
        success: (res) => {
          if (res.statusCode >= 200 && res.statusCode < 300) {
            resolve({
              objectKey: policy.objectKey,
              url: "",
              clientUploadedAt: new Date().toISOString(),
              originalFileName
            });
            return;
          }
          reject(new Error(`OSS 上传失败：${res.statusCode}`));
        },
        fail: reject
      });
    }));
}

function createDirectUploadPolicy(openid: string, fileName: string) {
  return request<DirectUploadPolicyResponse>({
    url: "/miniapp/v1/report-images/policy",
    method: "POST",
    data: {
      openid,
      fileName
    }
  });
}

function normalizeOssHost(host: string) {
  const normalizedHost = host.endsWith("/") ? host : `${host}/`;
  if (normalizedHost.startsWith("http://") || normalizedHost.startsWith("https://")) {
    return normalizedHost;
  }
  return `https://${normalizedHost}`;
}

function uploadReportImageThroughBackend(filePath: string): Promise<ReportImageUploadResponse> {
  return ensureOpenid().then((openid) => new Promise((resolve, reject) => {
    wx.uploadFile({
      url: `${getBaseUrl()}/api/miniapp/v1/report-images`,
      filePath,
      name: "file",
      formData: { openid },
      success: (res) => {
        try {
          const data = typeof res.data === "string" ? JSON.parse(res.data) : res.data;
          resolve(unwrapResponse<ReportImageUploadResponse>(data as ApiResponse<ReportImageUploadResponse>));
        } catch (error) {
          reject(error);
        }
      },
      fail: reject
    });
  }));
}

async function postMultipart<T>(options: {
  url: string;
  fields: Record<string, string>;
  files: Array<{
    fieldName: string;
    filePath: string;
    fileName: string;
    contentType: string;
  }>;
}): Promise<T> {
  const boundary = `----libuke-${Date.now()}-${Math.random().toString(16).slice(2)}`;
  const buildStartTime = Date.now();
  const body = await buildMultipartBody(boundary, options.fields, options.files);
  console.log("[upload] multipart built", {
    costMs: Date.now() - buildStartTime,
    bytes: body.byteLength,
    fileCount: options.files.length
  });

  return new Promise((resolve, reject) => {
    const requestStartTime = Date.now();
    wx.request({
      url: `${getBaseUrl()}/api${options.url}`,
      method: "POST",
      header: {
        "content-type": `multipart/form-data; boundary=${boundary}`
      },
      data: body,
      success: (res) => {
        console.log("[upload] request finished", {
          costMs: Date.now() - requestStartTime,
          statusCode: res.statusCode
        });
        try {
          resolve(unwrapResponse<T>(res.data as ApiResponse<T>));
        } catch (error) {
          reject(error);
        }
      },
      fail: reject
    });
  });
}

async function buildMultipartBody(
  boundary: string,
  fields: Record<string, string>,
  files: Array<{
    fieldName: string;
    filePath: string;
    fileName: string;
    contentType: string;
  }>
): Promise<ArrayBuffer> {
  const chunks: Uint8Array[] = [];

  Object.keys(fields).forEach((name) => {
    chunks.push(encodeUtf8(`--${boundary}\r\n`));
    chunks.push(encodeUtf8(`Content-Disposition: form-data; name="${name}"\r\n\r\n`));
    chunks.push(encodeUtf8(`${fields[name]}\r\n`));
  });

  for (const file of files) {
    chunks.push(encodeUtf8(`--${boundary}\r\n`));
    chunks.push(encodeUtf8(
      `Content-Disposition: form-data; name="${file.fieldName}"; filename="${file.fileName}"\r\n`
    ));
    chunks.push(encodeUtf8(`Content-Type: ${file.contentType}\r\n\r\n`));
    chunks.push(new Uint8Array(await readFileAsArrayBuffer(file.filePath)));
    chunks.push(encodeUtf8("\r\n"));
  }

  chunks.push(encodeUtf8(`--${boundary}--\r\n`));
  return concatChunks(chunks).buffer as ArrayBuffer;
}

function readFileAsArrayBuffer(filePath: string): Promise<ArrayBuffer> {
  return new Promise((resolve, reject) => {
    wx.getFileSystemManager().readFile({
      filePath,
      success: (res) => resolve(res.data),
      fail: reject
    });
  });
}

function concatChunks(chunks: Uint8Array[]) {
  const totalLength = chunks.reduce((sum, chunk) => sum + chunk.byteLength, 0);
  const result = new Uint8Array(totalLength);
  let offset = 0;

  chunks.forEach((chunk) => {
    result.set(chunk, offset);
    offset += chunk.byteLength;
  });

  return result;
}

function encodeUtf8(value: string) {
  const encoded = encodeURIComponent(value);
  const bytes: number[] = [];

  for (let index = 0; index < encoded.length; index++) {
    if (encoded[index] === "%") {
      bytes.push(parseInt(encoded.slice(index + 1, index + 3), 16));
      index += 2;
    } else {
      bytes.push(encoded.charCodeAt(index));
    }
  }

  return new Uint8Array(bytes);
}

function buildUploadFileName(filePath: string, index: number) {
  const suffix = filePath.toLowerCase().endsWith(".png") ? "png" : "jpg";
  return `report-${Date.now()}-${index}.${suffix}`;
}

function inferImageContentType(filePath: string) {
  return filePath.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
}
