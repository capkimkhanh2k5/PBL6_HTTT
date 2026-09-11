## 2026-09-10T04:11:30Z

You are Explorer 2 for Milestone 2 (Application Layer & Use Cases).
Working directory: /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/.agents/teamwork_preview_explorer_m2_2

MANDATORY FIRST STEP:
Read /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/.agents/ORIGINAL_REQUEST.md
Read /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/PROJECT.md

OBJECTIVE:
Investigate use case requirements, business logic, file validation, and interaction with FileStoragePort for Service Images.
Check:
1. FileStoragePort implemented in Milestone 1 (path, method signatures, return types).
2. Existing use cases in backend/src/main/java/com/danasea/backend/modules/service/application/
3. Exact business logic specifications for:
   - UploadServiceImageUseCase:
     * Vendor ownership check (how service ownership is verified vs current user/vendor).
     * File type validation (allowed MIME types / extensions: JPEG, PNG, WEBP).
     * Max images check (limit e.g. 10 images max per service; exception MaxImagesExceededException).
     * Sort order assignment (auto increment: max + 1 or count).
     * Calling FileStoragePort.uploadFile and creating ServiceImage.
   - DeleteServiceImageUseCase:
     * Ownership check.
     * Existence check and association check.
     * Deletion from repository (and optional Cloudinary cleanup).
   - ReorderServiceImagesUseCase:
     * Request DTO structure (e.g. list of reorder items with imageId and sortOrder).
     * IDOR prevention check: ensure ALL image IDs belong to the specified service. What happens if an image belongs to another service? (Throw UnauthorizedServiceAccessException or IllegalArgumentException).
     * Batch update sort_order.
4. Check domain exceptions in backend/src/main/java/com/danasea/backend/modules/service/domain/exceptions/.

SCOPE BOUNDARIES:
Read-only investigation. Do NOT modify or write any source code files.

OUTPUT REQUIREMENTS:
Write your findings to /Users/capkimkhanh/.gemini/antigravity/worktrees/PBL6/implement_service_assets_api/.agents/teamwork_preview_explorer_m2_2/handoff.md
Send a completion message back with the path to your handoff.md.
