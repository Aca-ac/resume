# AI 语音模拟面试模块 — 数据库设计（he）

## 1. 概述

本设计覆盖 Sprint 3「AI 语音模拟面试」后端所需表结构。语音识别/合成由 tian 的语音接口承担，本模块只持久化**会话状态机**与**对话记录**，供复盘（wang）与前端（yang）消费。

| 项目 | 值 |
| :--- | :--- |
| 数据库 | MySQL 8.0+ / InnoDB / utf8mb4 |
| 迁移 | Flyway：`V7__add_interview_tables.sql`（基础表）+ `V21__interview_state_machine.sql`（状态机扩展） |

## 2. 对话状态机（评审定稿）

```
PREPARING ──► QUESTIONING ◄──┐
                 │           │
                 ▼           │
             EVALUATING ─────┘  （未达 max_questions）
                 │
                 ▼ （达上限 / 主动结束）
             SUMMARIZING ──► COMPLETED
                 │
                 └──► ABORTED（用户中止或不可恢复错误）
```

| 状态 | 含义 | 允许的下一状态 |
| :--- | :--- | :--- |
| PREPARING | 已建会话，正在生成首题 | QUESTIONING / ABORTED |
| QUESTIONING | 已发出问题，等待候选人回答 | EVALUATING / SUMMARIZING / ABORTED |
| EVALUATING | 已收回答，评估并决定是否追问 | QUESTIONING / SUMMARIZING / ABORTED |
| SUMMARIZING | 生成面试总结（供复盘） | COMPLETED / ABORTED |
| COMPLETED | 正常结束 | — |
| ABORTED | 异常/用户中止 | — |

兼容字段 `status`：`ONGOING` 对应未结束态；`DONE` 对应 `COMPLETED`/`ABORTED`。

## 3. 表结构

### 3.1 面试会话表 `interview_sessions`

| 字段名 | 类型 | 约束 | 说明 |
| :--- | :--- | :--- | :--- |
| id | BIGINT UNSIGNED | PK AI | 会话 ID |
| user_id | BIGINT UNSIGNED | NOT NULL FK→users | 用户 |
| resume_id | BIGINT UNSIGNED | NOT NULL FK→resumes | 简历 |
| job_id | BIGINT UNSIGNED | NULL | 目标岗位（可选） |
| job_title | VARCHAR(255) | NOT NULL | 目标职位名 |
| status | VARCHAR(16) | NOT NULL | ONGOING / DONE（兼容） |
| state | VARCHAR(32) | NOT NULL | 状态机状态 |
| question_index | INT UNSIGNED | NOT NULL DEFAULT 0 | 已提问题数 |
| max_questions | INT UNSIGNED | NOT NULL DEFAULT 5 | 上限 |
| last_seq | INT UNSIGNED | NOT NULL DEFAULT 0 | WS 恢复游标 |
| jd_snapshot | MEDIUMTEXT | NULL | 开场 JD 快照 |
| report | MEDIUMTEXT | NULL | 面试总结原文 |
| ended_at | DATETIME | NULL | 结束时间 |
| created_at / updated_at | DATETIME | NOT NULL | 时间戳 |

### 3.2 面试对话表 `interview_messages`

| 字段名 | 类型 | 约束 | 说明 |
| :--- | :--- | :--- | :--- |
| id | BIGINT UNSIGNED | PK AI | 消息 ID |
| session_id | BIGINT UNSIGNED | NOT NULL FK | 会话 |
| role | VARCHAR(16) | NOT NULL | user / assistant / system |
| message_type | VARCHAR(32) | NOT NULL | QUESTION / ANSWER / FEEDBACK / SYSTEM / CHAT |
| question_index | INT UNSIGNED | NULL | 题号 |
| content | MEDIUMTEXT | NOT NULL | 正文 |
| evaluation | MEDIUMTEXT | NULL | 评估反馈 |
| seq | INT UNSIGNED | NOT NULL | 会话内单调序号 |
| client_msg_id | VARCHAR(64) | NULL UNIQUE(session,id) | 幂等 |
| sort_order | INT UNSIGNED | NOT NULL | 展示顺序 |
| created_at | DATETIME | NOT NULL | 时间 |

## 4. 与复盘模块边界

- he：写 `interview_sessions` / `interview_messages`，结束时写入 `report` 文本。
- wang：基于会话 ID 读取对话，生成复盘报告表 / 知识图谱（另表，本设计不包含）。
