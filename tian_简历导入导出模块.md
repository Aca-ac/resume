# 简历导入导出模块（后端）— tian

## 1️⃣ 数据库设计

**Flyway迁移文件** `server/src/main/resources/db/migration/V1__init.sql`

共4张表：

### users（用户表）
| 字段 | 类型 | 说明 |
|:----|:----|:------|
| id | BIGINT UNSIGNED AUTO_INCREMENT | 用户ID，主键 |
| email | VARCHAR(255) UNIQUE | 邮箱（登录账号） |
| password_hash | VARCHAR(255) | 密码哈希（bcrypt加密） |
| nickname | VARCHAR(100) NULL | 昵称 |
| name | VARCHAR(50) NULL | 姓名 |
| phone | VARCHAR(20) NULL | 手机号 |
| birth_date | DATE NULL | 出生日期 |
| education | VARCHAR(50) NULL | 最高学历 |
| work_years | TINYINT UNSIGNED NULL | 工作年限 |
| city | VARCHAR(50) NULL | 所在城市 |
| status | TINYINT UNSIGNED | 账号状态：0-禁用，1-正常 |
| last_login_at | DATETIME NULL | 最后登录时间 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### resumes（简历主表）
| 字段 | 类型 | 说明 |
|:----|:----|:------|
| id | BIGINT UNSIGNED AUTO_INCREMENT | 简历ID，主键 |
| user_id | BIGINT UNSIGNED NOT NULL | 用户ID，外键 → users(id) CASCADE |
| title | VARCHAR(255) | 简历标题，默认"未命名简历" |
| version | INT UNSIGNED | 版本号，默认1 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### resume_details（简历明细表——分段存储）
| 字段 | 类型 | 说明 |
|:----|:----|:------|
| id | BIGINT UNSIGNED AUTO_INCREMENT | 明细ID，主键 |
| resume_id | BIGINT UNSIGNED NOT NULL | 简历ID，外键 → resumes(id) CASCADE |
| section_type | VARCHAR(32) | 分段类型：EDUCATION / WORK_EXPERIENCE / PROJECT / SKILL / SUMMARY |
| section_name | VARCHAR(64) | 分段标题，如"教育经历""工作经历" |
| content | TEXT | 分段内容（JSON格式结构化存储） |
| sort_order | INT UNSIGNED | 排序序号 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### resume_files（简历文件表）
| 字段 | 类型 | 说明 |
|:----|:----|:------|
| id | BIGINT UNSIGNED AUTO_INCREMENT | 文件ID，主键 |
| user_id | BIGINT UNSIGNED NOT NULL | 用户ID，外键 → users(id) CASCADE |
| resume_id | BIGINT UNSIGNED NULL | 关联简历ID，外键 → resumes(id) SET NULL |
| file_type | VARCHAR(32) | 文件类型：JPG / PNG / PDF / DOCX |
| file_path | VARCHAR(512) | 文件存储路径 |
| file_size | BIGINT UNSIGNED | 文件大小（字节） |
| original_name | VARCHAR(255) | 原始文件名 |
| ocr_text | TEXT NULL | OCR识别结果 |
| created_at | DATETIME | 创建时间 |

---

## 2️⃣ 实体类 & Mapper

路径：`server/src/main/java/com/resume/module/resume/`

```
entity/
├── Resume.java         — 简历主表实体
├── ResumeDetail.java   — 简历明细实体
└── ResumeFile.java     — 文件存储实体

mapper/
├── ResumeMapper.java
├── ResumeDetailMapper.java
└── ResumeFileMapper.java
```

---

## 3️⃣ 接口开发

路径：`server/src/main/java/com/resume/module/resume/`

```
service/ResumeService.java       — 业务逻辑
controller/ResumeController.java — RESTful API
```

### 简历CRUD

| 方法 | 路径 | 功能 |
|:----|:-----|:------|
| POST | `/api/v1/resumes` | 创建简历 |
| GET | `/api/v1/resumes` | 获取简历列表 |
| GET | `/api/v1/resumes/{id}` | 获取简历详情 |
| PUT | `/api/v1/resumes/{id}` | 修改简历标题 |
| DELETE | `/api/v1/resumes/{id}` | 删除简历 |

### 简历明细分段管理

| 方法 | 路径 | 功能 |
|:----|:-----|:------|
| GET | `/api/v1/resumes/{resumeId}/details` | 获取分段列表 |
| POST | `/api/v1/resumes/{resumeId}/details` | 添加分段 |
| PUT | `/api/v1/resumes/details/{detailId}` | 修改分段内容 |
| DELETE | `/api/v1/resumes/details/{detailId}` | 删除分段 |

### 文件上传与OCR

| 方法 | 路径 | 功能 |
|:----|:-----|:------|
| POST | `/api/v1/resumes/{resumeId}/files` | 上传简历文件 |
| GET | `/api/v1/resumes/files` | 文件列表 |
| POST | `/api/v1/resumes/files/{fileId}/ocr` | **JPG/PNG图片OCR识别** |

---

## 4️⃣ OCR集成

已对接**通义千问 Qwen3.5-OCR API**

**流程：**
1. 用户上传JPG/PNG图片
2. 调用 `POST /api/v1/resumes/files/{fileId}/ocr` 触发OCR识别
3. 后端读取图片文件 → Base64编码 → 调用通义千问 `qwen3.5-ocr` 模型
4. 识别结果存入 `resume_files.ocr_text` 字段
5. 前端展示结果，用户手动修正

**配置：**
- 环境变量：`DASHSCOPE_API_KEY`（已配置在系统环境变量中）
- 模型：`qwen3.5-ocr`
- API地址：`https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions`

---

## 5️⃣ 文件存储方案

| 项目 | 说明 |
|:----|:------|
| 存储位置 | `uploads/resumes/` 目录（可配置） |
| 文件命名 | UUID重命名，防冲突 |
| 支持类型 | JPG / PNG / PDF / DOCX |
| 大小限制 | 10MB（可配置） |
| 配置类 | `server/src/main/java/com/resume/config/StorageProperties.java` |

---

## 6️⃣ 涉及文件清单

```
server/src/main/resources/db/migration/V1__init.sql
server/src/main/java/com/resume/module/resume/entity/Resume.java
server/src/main/java/com/resume/module/resume/entity/ResumeDetail.java
server/src/main/java/com/resume/module/resume/entity/ResumeFile.java
server/src/main/java/com/resume/module/resume/mapper/ResumeMapper.java
server/src/main/java/com/resume/module/resume/mapper/ResumeDetailMapper.java
server/src/main/java/com/resume/module/resume/mapper/ResumeFileMapper.java
server/src/main/java/com/resume/module/resume/service/ResumeService.java
server/src/main/java/com/resume/module/resume/controller/ResumeController.java
server/src/main/java/com/resume/config/StorageProperties.java
```