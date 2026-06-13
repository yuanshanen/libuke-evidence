# 物业服务不达标取证小程序

当前目录是微信原生小程序 V1 工程。

## 技术选型

- 微信原生小程序
- TypeScript
- WXML
- WXSS
- 微信原生 API

## 页面范围

- `pages/report/index`：首页，即问题上报页面
- `pages/records/index`：上报记录页面

当前 UI V1 不包含后台台账页面和“我的”页面。

## 导入方式

使用微信开发者工具导入本目录：

```text
C:\Users\30418\Desktop\html-ui-prototyper\miniprogram
```

`project.config.json` 中当前使用 `touristappid`，正式联调时替换为真实小程序 AppID。

## 当前交互

- 首页未绑定小区时展示“未选择小区”
- 点击“选择”打开邀请码弹窗
- 问题大类和常见问题为级联选择
- 地理位置使用地图区域占位
- 图片附件支持选择 1-4 张图片
- 记录页通过带搜索图标的下拉框按问题大类筛选
