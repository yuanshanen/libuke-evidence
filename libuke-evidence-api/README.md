# libuke-evidence-api

邻应台后端服务，提供小程序接口、后台管理接口和开放接口。

## 接口前缀

- 小程序接口：`/api/miniapp/v1`
- 后台接口：`/api/admin/v1`
- 开放接口：`/api/open/v1`

## 技术栈

- Java 21
- Spring Boot
- MyBatis Plus
- MySQL
- Maven
- Apache POI

## 启动

```bash
mvn spring-boot:run
```

## 必要环境变量

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3306/libuke_evidence?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=your-password
AMAP_MAP_KEY=your-amap-key
AMAP_JS_API_KEY=your-amap-js-key
AMAP_JS_API_SECURITY_KEY=your-amap-security-key
WECHAT_MINIAPP_APP_ID=your-miniapp-app-id
WECHAT_MINIAPP_APP_SECRET=your-miniapp-app-secret
```

## 说明

仓库不提交真实密码、地图 Key、微信小程序 Secret、OSS Secret。部署时请通过环境变量或平台密钥管理注入。
