# AI 语音模拟面试模块 — 接口设计（he）

## 1. 模块概述

负责对话状态机、问题生成、回答评估、面试结束与历史查询，以及 WebSocket 实时推送。  
**不含**语音识别/合成（见 tian《语音接口联调文档》）。

认证：除 WebSocket 握手可用 `?token=` 外，其余 REST 需 `Authorization: Bearer <JWT>`。

## 2. 接口列表

| 路径 | 方法 | 说明 |
| :--- | :--- | :--- |
| `/api/v1/interview/start` | POST | 开始面试，生成首题，进入 QUESTIONING |
| `/api/v1/interview/{sessionId}/answer` | POST | 提交回答，评估并追问或进入收尾 |
| `/api/v1/interview/{sessionId}/question` | POST | 单独生成下一题（重试/补题） |
| `/api/v1/interview/{sessionId}/end` | POST | 主动结束，生成总结 |
| `/api/v1/interview/{sessionId}` | GET | 会话详情（含状态机字段） |
| `/api/v1/interview/{sessionId}/messages` | GET | 分页拉消息（断线恢复也可用） |
| `/api/v1/interview/history` | GET | 当前用户面试历史 |
| `/api/v1/interview/{sessionId}/report` | GET | 获取/触发生成总结（兼容旧前端） |
| `/ws/interview` | WS | 实时推送与恢复 |

## 3. REST 详细设计

### 3.1 POST `/api/v1/interview/start`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
| :--- | :--- | :--- | :--- |
| resumeId | Long | 是 | 简历 ID |
| jobTitle | String | 否 | 职位名；缺省「通用岗位」 |
| jobId | Long | 否 | 目标岗位 ID；有则快照 JD |
| maxQuestions | Integer | 否 | 默认 5，范围 3–10 |

**响应 data（InterviewSessionVO）**

```json
{
  "id": 1001,
  "resumeId": 12,
  "jobId": 33,
  "jobTitle": "Java后端开发",
  "status": "ONGOING",
  "state": "QUESTIONING",
  "questionIndex": 1,
  "maxQuestions": 5,
  "lastSeq": 1,
  "report": null,
  "createdAt": "2026-07-16 18:00:00",
  "firstQuestion": {
    "role": "assistant",
    "messageType": "QUESTION",
    "questionIndex": 1,
    "content": "请介绍一下你最近负责的后端项目。",
    "seq": 1
  }
}
```

### 3.2 POST `/api/v1/interview/{sessionId}/answer`

**请求体**

| 字段 | 类型 | 必填 | 说明 |
| :--- | :--- | :--- | :--- |
| answer | String | 是 | 文字回答（语音需先走 tian ASR） |
| clientMsgId | String | 否 | 幂等键 |

**响应 data**

```json
{
  "state": "QUESTIONING",
  "questionIndex": 2,
  "finished": false,
  "userMessage": { "role": "user", "messageType": "ANSWER", "content": "...", "seq": 2 },
  "feedback": { "role": "assistant", "messageType": "FEEDBACK", "content": "优点…改进…", "seq": 3 },
  "nextQuestion": { "role": "assistant", "messageType": "QUESTION", "questionIndex": 2, "content": "…", "seq": 4 }
}
```

若已达上限：`finished=true`，`state=COMPLETED`，`nextQuestion=null`，并带 `report` 摘要字段。

### 3.3 POST `/api/v1/interview/{sessionId}/end`

主动结束：`QUESTIONING|EVALUATING` → `SUMMARIZING` → `COMPLETED`。

### 3.4 GET `/api/v1/interview/history`

| 参数 | 说明 |
| :--- | :--- |
| page / size | 分页，默认 1 / 20 |

返回会话列表（不含完整消息）。

### 3.5 GET `/api/v1/interview/{sessionId}/messages`

| 参数 | 说明 |
| :--- | :--- |
| afterSeq | 可选；只返回 `seq > afterSeq`（断线恢复） |
| page / size | 兼容旧分页 |

## 4. WebSocket 协议

**连接**：`ws(s)://{host}/ws/interview?token={JWT}&sessionId={id}`

### 4.1 客户端 → 服务端

| type | 含义 | payload |
| :--- | :--- | :--- |
| PING | 心跳 | `{}` |
| ANSWER | 等价 REST answer | `{ "answer":"...", "clientMsgId":"..." }` |
| END | 主动结束 | `{}` |
| RESUME | 断线恢复 | `{ "afterSeq": 12 }` |

### 4.2 服务端 → 客户端

| type | 含义 |
| :--- | :--- |
| PONG | 心跳回复 |
| STATE | 状态变更 `{ state, questionIndex, maxQuestions }` |
| MESSAGE | 单条消息（同 MessageVO + seq） |
| ANSWER_RESULT | 同 REST answer 响应 |
| ERROR | `{ code, message }` |
| SNAPSHOT | RESUME 后批量 `{ messages:[...], lastSeq }` |

消息体统一 JSON：`{ "type":"MESSAGE", "sessionId":1001, "payload":{...} }`。

### 4.3 断线重连

1. 前端重连同一 `sessionId`，发 `RESUME.afterSeq = 本地 lastSeq`。  
2. 服务端返回 `SNAPSHOT`（`seq > afterSeq`）再推 `STATE`。  
3. REST `GET .../messages?afterSeq=` 可作为 HTTP 兜底。

## 5. 错误码

| code | 场景 |
| :--- | :--- |
| 400 | 空回答 / 状态不允许作答 / 参数非法 |
| 404 | 会话或简历不存在/无权 |
| 409 | 状态机非法迁移 |
| 500 | AI Key 未配置 / 模型调用失败 |
