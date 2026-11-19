# Báo Cáo Đánh Giá An Ninh Ứng Dụng (AppSec Audit) - Phase 3: Cloud Readiness

- **Dự án:** SoodFolio API (`soodfolio-api`)
- **Vai trò:** `security-auditor`
- **Thời gian đánh giá:** 2026-11-19
- **Phạm vi:** Hạ tầng Cloud Readiness (Cloudinary File Storage, Brevo Transactional Email, Neon Flyway Migration, Actuator & Prod Profile)
- **Kết luận chung:** **PASS (ĐẠT)** - 0 Critical, 0 High, 0 Medium findings. Sẵn sàng triển khai an toàn lên Render/Railway & Neon PostgreSQL.

---

## 1. Tóm Tắt Kết Quả Kiểm Toán (Executive Summary)

| Hạng mục kiểm toán | Tiêu chuẩn đánh giá | Kết quả | Ghi chú & Biện pháp kiểm soát |
| :--- | :--- | :---: | :--- |
| **1. Secret Hygiene** | Không rò rỉ credential, API key, password | **ĐẠT** | 100% secret được externalize qua biến môi trường. Không hardcode key. Exception/logs đã được làm sạch hoàn toàn. |
| **2. Upload Security** | Chống MIME-spoofing, Polyglot, Path Traversal | **ĐẠT** | Kiểm tra nghiêm ngặt Magic Bytes (JPEG, PNG, GIF, WEBP, PDF). Phân quyền `raw` cho PDF, chống traversal trên Local provider. |
| **3. Password Reset & Email** | Chống Account Enumeration & Host Poisoning | **ĐẠT** | Reset link sinh từ `FRONTEND_BASE_URL`, sanitize HTML name, phản hồi đồng nhất dù email tồn tại hay không. |
| **4. Actuator & Prod Profile** | Giảm thiểu bề mặt tấn công hạ tầng (Attack Surface) | **ĐẠT** | Chỉ mở `/actuator/health` ẩn danh với `show-details: never`. Tắt Swagger/OpenAPI ở prod. Tách kênh Flyway Direct URL. |

---

## 2. Chi Tiết Từng Hạng Mục Kiểm Toán

### 2.1. Secret Hygiene & Data Leakage Prevention
- **Cấu hình Spring Environment:**
  - `application.yml` và `application-prod.yml` không chứa bất kỳ giá trị secret mặc định nguy hiểm nào (`${CLOUDINARY_API_SECRET:}`, `${BREVO_API_KEY:}`, `${DB_PASSWORD}`).
  - Trong `test/resources/application.yml`, giá trị mock secret JWT được đánh dấu rõ ràng với `# gitleaks:allow` và chỉ phục vụ offline unit testing.
- **Log & Error Message Sanitization:**
  - `CloudinaryFileStorageProvider`: Nếu môi trường truyền `CLOUDINARY_URL` không đúng chuẩn, provider ném ngoại lệ chung `"Invalid CLOUDINARY_URL format"`, tuyệt đối không nối chuỗi URL vào exception để phòng ngừa in lộ `api_secret`.
  - `BrevoEmailServiceImpl`: Chỉ log mã HTTP status (`status=401`, `status=500`) hoặc lớp ngoại lệ mạng khi xảy ra lỗi. Tuyệt đối không log `api-key`, `resetLink` hay `rawToken`.
  - `AuthServiceImpl`: `resetLink` chỉ được in ra log trong môi trường dev khi cờ `app.auth.log-reset-link=true` (mặc định luôn là `false` ở production).

### 2.2. File Upload Security & Magic Bytes Anti-Spoofing
- **Magic Bytes Validation:**
  - Không dựa vào header `Content-Type` do client gửi lên hoặc đuôi file dễ bị làm giả.
  - Kiểm tra trực tiếp byte header từ nội dung file:
    - **JPEG:** `FF D8 FF` (hỗ trợ cả `.jpg` và `.jpeg`).
    - **PNG:** `89 50 4E 47 0D 0A 1A 0A` (8 byte header chuẩn).
    - **GIF:** `GIF87a` hoặc `GIF89a` (6 byte header chuẩn).
    - **WEBP:** Kiểm tra `RIFF` ở đầu và `WEBP` tại vị trí byte 8..11.
    - **PDF:** `%PDF-` (5 byte header chuẩn).
  - Tệp SVG bị chặn tuyệt đối (`BadRequestException`) để phòng chống XSS qua XML/SVG embedding.
- **Storage Routing & Separation:**
  - Ảnh được định tuyến về `resource_type=image` trên Cloudinary với `public_id` đã loại bỏ phần mở rộng.
  - CV / Resume PDF được định tuyến về `resource_type=raw` với `public_id` giữ nguyên đuôi `.pdf` nhằm tuân thủ quy tắc bảo mật và chính sách gói dịch vụ.
  - `LocalFileStorageProvider` kiểm tra nghiêm ngặt `Path.normalize()` và `startsWith(rootUploadPath)` để triệt tiêu lỗ hổng Path Traversal (`../../`).

### 2.3. Password Reset Flow & Email Transport Security
- **Anti-Account Enumeration:**
  - Phương thức `forgotPassword()` trả về cùng một thông điệp phản hồi `MessageResponse("Nếu email tồn tại trong hệ thống, link khôi phục mật khẩu đã được tạo thành công.")` bất kể email có tồn tại trong cơ sở dữ liệu hay không.
  - Khi email không tồn tại, quy trình không gọi Brevo REST API, không để lộ độ trễ bất thường.
- **Phòng Chống Host Header Poisoning:**
  - Reset link được xây dựng từ cấu hình đáng tin cậy phía máy chủ `${app.frontend.base-url}` (`FRONTEND_BASE_URL`), hoàn toàn không tin cậy hay lấy từ request headers `Host` hay `X-Forwarded-Host` của client.
- **HTML Injection & Template Injection Prevention:**
  - Tên người dùng `recipientName` được escape toàn bộ ký tự HTML đặc biệt (`&`, `<`, `>`, `"`, `'`) trước khi ghép vào chuỗi HTML email gửi qua Brevo.
- **Token Hashing & Expiration:**
  - Token thô (`rawToken` 64 ký tự hex ngẫu nhiên an toàn) chỉ xuất hiện trong link gửi email cho người dùng; cơ sở dữ liệu chỉ lưu trữ băm SHA-256 (`token_hash`) với thời gian hết hạn nghiêm ngặt 15 phút.

### 2.4. Production Infrastructure & Attack Surface Reduction
- **Actuator Healthcheck Exposure:**
  - `management.endpoints.web.exposure.include: health` chỉ mở duy nhất endpoint kiểm tra sức khỏe.
  - `management.endpoint.health.show-details: never` đảm bảo không để lộ thông tin chi tiết về ổ đĩa, cơ sở dữ liệu nội bộ hay heap memory ra bên ngoài.
  - `WebSecurityConfig` cho phép truy cập ẩn danh đối với `/actuator/health` và `/actuator/health/**` (phục vụ healthcheck của Render/Railway); toàn bộ các endpoint quản trị nhạy cảm khác (`/actuator/env`, `/actuator/beans`, `/actuator/configprops`) đều bị chặn và yêu cầu xác thực JWT.
- **Swagger / OpenAPI Tắt ở Production:**
  - Trong `application-prod.yml`, `springdoc.api-docs.enabled: false` và `springdoc.swagger-ui.enabled: false` giúp đóng toàn bộ tài liệu API công khai khi chạy live trên Internet.
- **Flyway & Neon PostgreSQL Resilience:**
  - Tách `spring.flyway.url: ${FLYWAY_DB_URL:...}` để cho phép cấu hình kết nối Direct (không qua PgBouncer pooler) cho DDL migrations.
  - `connection-timeout: 20000` (20 giây) ở HikariCP giúp ứng dụng khởi động và kết nối ổn định khi cơ sở dữ liệu Neon thức giấc từ trạng thái scale-to-zero (cold wake-up).
  - Kích hoạt `server.shutdown: graceful` và `server.forward-headers-strategy: framework` hỗ trợ reverse proxy trên cloud.

---

## 3. Khuyến Nghị Vận Hành Trên Production (Deployment Guidance)

1. **Biến môi trường trên Render / Railway:**
   - Cung cấp `DB_URL` (kèm `sslmode=require` khi kết nối Neon).
   - Cung cấp `FLYWAY_DB_URL` trỏ tới Neon Direct URL (nếu dùng PgBouncer cho connection pooling).
   - Thiết lập `SPRING_PROFILES_ACTIVE=prod`.
   - Cung cấp `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`.
   - Cung cấp `BREVO_API_KEY`, `MAIL_SENDER_EMAIL` (đã verify domain trên Brevo), `FRONTEND_BASE_URL`.
2. **Health Check Path trên Render/Railway:**
   - Đặt đường dẫn: `/api/v1/actuator/health`.
