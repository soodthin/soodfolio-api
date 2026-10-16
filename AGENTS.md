# SOODFOLIO-API AGENT RULES & WORKFLOW

## 1. Single Source of Truth & Memory Rules
- **Tài liệu Backend:** Mọi thay đổi về entity, logic, DTO, endpoint REST phải được cập nhật vào `D:\Project\SoodFolio\docs\document-for-backend-java`.
- **Handoff:** Bàn giao tiến độ vào `D:\Project\SoodFolio\docs\handoff.md`.
- **Postman Sync (BẮT BUỘC):** Khi hoàn thành bất kỳ endpoint mới nào, Agent phải tự động bổ sung hoặc cập nhật Postman request test vào `D:\Project\SoodFolio\soodfolio-api\docs\postman\collections\SoodFolio API\`.

## 2. Coding Standards
- **REST & Security:** Mọi mutating endpoint phải bảo mật qua `@AuthenticationPrincipal UserPrincipal`, chống IDOR bằng cách bind `portfolioId` với `userId` trong database.
- **DTO Pattern:** Không để lộ Entity ra ngoài Controller. Luôn validate payload qua `@Valid`.
- **TDD:** Triển khai Red -> Green loop tại service layer test trước khi hoàn thiện controller.
- **Ponytail:** Viết mã tối giản, surgical change, không thêm abstraction thừa.
