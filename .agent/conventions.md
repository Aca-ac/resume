# 项目开发规范

## 1. Git 工作流规范

### 1.1 分支策略
- **main**：稳定生产分支，只接受 Merge Request，禁止直接推送
- **dev-\<name\>**：个人开发分支（dev-tian、dev-he、dev-wang、dev-yang）
- **gitlab-deploy**：CI/CD 部署分支

### 1.2 提交信息格式
`
<type>(<scope>): <subject>
`
| Type     | 说明         |
|----------|-------------|
| feat     | 新功能       |
| fix      | Bug 修复     |
| docs     | 文档变更     |
| refactor | 代码重构     |
| test     | 测试相关     |
| chore    | 构建/工具    |
| style    | 格式调整     |

示例：
`
feat(resume): 新增简历 PDF 导出功能
fix(auth): 修复 Token 过期未跳转问题
docs(readme): 更新 API 文档
`

### 1.3 操作流程
`ash
# 每日开始
git checkout dev-tian
git pull origin dev-tian

# 提交代码
git add .
git commit -m "feat(模块): 描述内容"
git push origin dev-tian

# 合并到 main
# → 在 GitLab 上发起 Merge Request，由他人 Review 后合并
`

## 2. 代码规范

### 2.1 后端（Java / Spring Boot）

**包结构**
`
com.resume
├── controller        # 控制器层
├── service           # 业务逻辑层
├── mapper            # 数据访问层（MyBatis-Plus）
├── model
│   ├── entity        # 数据库实体
│   ├── dto           # 数据传输对象
│   └── vo            # 视图对象
├── config            # 配置类
├── common
│   ├── exception     # 全局异常处理
│   ├── result        # 统一返回封装
│   └── constant      # 常量
└── util              # 工具类
`

**命名规范**
| 元素       | 规范         | 示例                          |
|-----------|-------------|------------------------------|
| 类名       | 大驼峰       | ResumeController             |
| 方法名     | 小驼峰       | createResume()               |
| 变量名     | 小驼峰       | userName                     |
| 常量       | 全大写下划线  | MAX_FILE_SIZE                |
| REST 路径  | 名词复数      | /api/v1/resumes              |

**统一返回格式**
`json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1700000000000
}
`

### 2.2 前端（Vue 3 / TypeScript）

**目录结构**
`
src
├── api          # API 请求封装
├── views        # 页面组件
├── components   # 公共组件
├── stores       # Pinia 状态管理
├── router       # 路由配置
├── utils        # 工具函数
└── types        # TypeScript 类型定义
`

**命名规范**
| 元素       | 规范         | 示例                          |
|-----------|-------------|------------------------------|
| 组件文件   | 大驼峰       | ResumeEditor.vue             |
| 普通文件   | 小驼峰       | authApi.ts                   |
| 变量/函数  | 小驼峰       | getUserInfo()                |
| 组件名     | 多单词       | <ResumeEditor />             |

### 2.3 数据库
- 表名：小写 + 下划线（resume_info）
- 字段名：小写 + 下划线（user_name）
- 索引命名：idx_表名_字段名
- 每张表必须包含 id（自增主键）、create_time、update_time

## 3. API 规范

- **基础路径**：/api/v1/
- **认证方式**：JWT Token，请求头 Authorization: Bearer <token>
- **分页参数**：统一 page（从1开始）和 size

**RESTful 设计**
| 方法   | 用途   | 示例                              |
|-------|--------|----------------------------------|
| GET   | 查询   | GET /api/v1/resumes?page=1&size=10 |
| POST  | 创建   | POST /api/v1/resumes             |
| PUT   | 更新   | PUT /api/v1/resumes/{id}         |
| DELETE| 删除   | DELETE /api/v1/resumes/{id}      |

## 4. 开发流程规范

### 4.1 功能开发流程
1. 确认需求 → 评估影响范围
2. 在个人分支 dev-xxx 上开发
3. 本地自测通过
4. 发起 MR → 指定至少一人 Review
5. Review 通过后合并到 main

### 4.2 测试要求
- 后端：使用 Maven 执行 mvn test
- 关键接口需补充单元测试

### 4.3 环境配置
- 配置文件通过 .env 传递，禁止提交敏感信息到仓库
- 环境变量统一在 application.yml 中引用占位符

## 5. AI 使用规范

- AI 生成的代码必须经人工审查后方可提交
- 核心业务逻辑（匹配算法、评分模型）由人工编写
- Prompt 模板统一管理在 server/src/main/resources/ai/ 目录
- API 密钥通过环境变量传入，禁止硬编码在代码中
