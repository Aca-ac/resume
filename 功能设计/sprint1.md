# Sprint 1 开发计划

## 一、迭代目标

完成 **用户认证模块**、**简历导入与管理模块**、**AI简历智能分析模块** 三大核心功能的开发，实现用户邮箱注册登录、简历多格式导入与分段管理、AI多维度评分与基于STAR法则的优化建议功能。简历的模板化导出延后至 Sprint 2。

## 二、模块任务清单

### 1. 用户认证模块（对应架构文档 4.2.1）

**后端任务：**
- 设计用户表结构（id、email、password_hash、nickname、name、phone、birth_date、education、work_years、city、status、last_login_at、created_at、updated_at）
- 基于 Spring Security 实现账号注册登录流程
- 实现邮箱验证码发送接口（调用第三方邮件服务）
- 实现用户注册接口（邮箱+密码+验证码验证）
- 实现用户登录接口（邮箱+密码验证，生成 JWT Token）
- 实现 Token 刷新接口（Refresh Token 机制）
- 实现个人信息 CRUD 接口
- 配置 JWT 鉴权与权限拦截

**前端任务：**
- 开发注册页面（邮箱、密码、验证码输入表单）
- 开发登录页面（邮箱、密码输入表单）
- 开发个人中心页面（姓名、手机号、出生日期、学历、工作年限、城市表单）
- 实现 Token 过期自动刷新逻辑（无感知刷新）
- 实现个人信息与简历编辑表单的关联自动填充（如姓名、电话等字段）

---

### 2. 简历导入与管理模块（对应架构文档 4.1.2、4.1.3、4.2.2、4.2.4）

> 简历的模板化导出（模板选择、自动填充、按模板导出 Word/PDF）见 Sprint 2。

**后端任务：**
- 设计简历主表结构（id、user_id、title、source_type、version、created_at、updated_at）
- 设计简历明细表结构（分段存储摘要、教育经历、工作经历、项目经验、技能）
- 设计简历文件表结构（上传文件、OCR/解析文本、解析状态）
- 实现简历 CRUD 接口（创建、列表查询、详情查询、修改标题、删除）
- 实现简历明细分段管理接口（各分段的增删改查与排序）
- 实现 Word 文档解析接口（.doc/.docx 格式文本提取，保留段落结构）
- 实现 PDF 文档解析接口（文本提取）
- 实现 JPG 图片 OCR 识别接口（调用 OCR 服务，结果供用户手动修正）
- 设计文件临时存储方案（UUID 命名、大小限制、MD5 秒传去重）

**前端任务：**
- 开发简历列表页面（展示用户所有简历，支持新建、删除）
- 开发简历详情与分段编辑页面（教育经历、工作经历、项目经验、技能分段编辑）
- 开发简历导入页面（支持选择 .doc/.docx/.pdf/.jpg 文件）
- 开发 OCR 识别结果展示与手动修正页面
- 实现导入进度与状态提示

---

### 3. AI简历智能分析模块（对应架构文档 4.1.4、4.2.3、4.2.5）

**后端任务：**
- 设计分析记录表结构（id、user_id、resume_id、各维度评分、total_score、各维度权重、suggestions、status、error_message、request_id、created_at、updated_at）
- 实现简历多维度权重评分算法（Summary、Education、Experience、Skill、Project 五个维度，各维度 0-100 分，按权重加权计算总分）
- 实现大模型 API 调用封装（阿里云 API，管理 Prompt 模板，处理请求限流与结果解析）
- 实现 AI 分析接口（调用大模型生成修改建议）
- 基于 STAR 法则（情境-任务-行动-结果）生成结构化修改建议，引导用户量化经历描述
- 实现分析历史记录查询接口

**前端任务：**
- 开发 AI 分析触发按钮与加载状态提示（5-15 秒加载）
- 使用 ECharts 实现雷达图评分展示（5 个维度，支持鼠标悬停查看分数与权重）
- 开发修改建议列表展示（原文定位、问题描述、优化建议、修改示例）
- 实现"应用建议"一键应用功能
- 实现"忽略"跳过建议功能
- 开发分析历史记录页面

---

### 4. 基础设施与公共模块（对应架构文档 4.1.11、4.2.11）

**后端任务：**
- 搭建 SpringBoot 3.x 项目基础框架
- 配置数据库连接（MySQL）
- 集成 MyBatis-Plus ORM 框架
- 配置 Knife4j / Swagger 接口文档
- 实现统一返回封装
- 实现全局异常处理
- 实现参数校验

**前端任务：**
- 搭建 Vue3 项目基础框架（Vite + Vue3 + Element Plus + Pinia）
- 配置路由与权限拦截
- 开发全局布局组件
- 配置 Axios 请求封装与拦截器
- 配置样式主题

## 三、验收标准

### 用户注册与登录
| 编号 | 验收条件 |
|------|----------|
| AC1.1 | 用户输入有效邮箱和密码后，系统发送6位数字验证码至该邮箱，输入正确验证码后完成注册 |
| AC1.2 | 注册成功后，用户可使用邮箱+密码登录，登录后生成 JWT Token 用于后续请求鉴权 |
| AC1.3 | Token 过期后，系统自动使用 Refresh Token 刷新，用户无感知（无需重新登录） |
| AC1.4 | 登录成功后进入个人中心，用户可填写/修改：姓名、手机号、出生日期、最高学历、工作年限、所在城市 |
| AC1.5 | 个人中心信息保存后，在简历编辑表单中可自动读取并填充（如姓名、电话等字段） |

### 简历导入与管理
| 编号 | 验收条件 |
|------|----------|
| AC2.1 | 用户可在简历列表页新建、查看、删除简历，简历支持多版本保存 |
| AC2.2 | 用户可对简历进行分段编辑（教育经历、工作经历、项目经验、技能），保存后版本号自增 |
| AC2.3 | 用户点击"导入简历"，支持选择 .doc / .docx / .pdf / .jpg 格式文件上传 |
| AC2.4 | Word 文件导入后，系统能提取正文文本，保留段落结构，准确率≥90% |
| AC2.5 | PDF 文件导入后，系统能提取正文文本，准确率≥90% |
| AC2.6 | JPG 文件导入后，系统调用 OCR 识别文字，准确率≥70%（展示识别结果供用户手动修正） |

### AI简历智能分析
| 编号 | 验收条件 |
|------|----------|
| AC3.1 | 用户点击"AI智能分析"后，系统在5-15秒内返回分析结果（含加载中状态提示） |
| AC3.2 | 雷达图展示5个维度的评分：Summary（个人总结）、Education（教育背景）、Experience（工作经历）、Skill（技能）、Project（项目经验），每个维度0-100分，按权重加权得出总分 |
| AC3.3 | 雷达图使用 ECharts 渲染，支持鼠标悬停查看具体分数与权重 |
| AC3.4 | 分析结果下方列出修改建议列表，每条建议包含：原文定位（如"工作经历-第3行"）、问题描述、优化建议、修改示例 |
| AC3.5 | 修改建议至少包含5条，覆盖至少4个维度 |
| AC3.6 | 用户可点击"应用建议"一键将修改示例应用到简历中，或点击"忽略"跳过该条建议 |
| AC3.7 | 分析记录存入数据库，用户可在历史记录中查看过往分析报告 |
| AC3.8 | 修改建议基于 STAR 法则（情境-任务-行动-结果）生成，引导用户结构化、量化地描述经历 |

## 四、数据结构设计

### 核心数据表
- **用户表（users）**：id、email、password_hash、nickname、name、phone、birth_date、education、work_years、city、status、last_login_at、created_at、updated_at
- **简历主表（resumes）**：id、user_id、title、source_type、version、created_at、updated_at
- **简历明细表（resume_details）**：id、resume_id、section_type（SUMMARY/EDUCATION/WORK_EXPERIENCE/PROJECT/SKILL）、section_name、content、sort_order、created_at、updated_at
- **简历文件表（resume_files）**：id、user_id、resume_id、file_type、file_path、file_size、file_md5、original_name、ocr_text、parse_status、created_at
- **分析记录表（analysis_records）**：id、user_id、resume_id、各维度评分（summary_score~project_score）、total_score、各维度权重（weight_summary~weight_project）、suggestions、status、error_message、request_id、created_at、updated_at