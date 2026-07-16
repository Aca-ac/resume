# AI 语音模拟面试 — 对话状态机设计（he，开发前评审定稿）

## 1. 目标

将面试过程从「无状态多轮 chat」升级为可审计、可恢复的状态机，支撑：

- 进度条 / 题号展示（AC1.5）
- 正常结束与主动结束
- WebSocket 断线恢复（按 seq）
- 复盘模块消费完整对话

## 2. 状态定义

| 状态 | 说明 |
| --- | --- |
| PREPARING | 会话已创建，正在生成首题 |
| QUESTIONING | 已向候选人发出问题，等待回答 |
| EVALUATING | 已收到回答，正在评估 / 决策是否追问 |
| SUMMARIZING | 正在生成面试总结 |
| COMPLETED | 正常结束 |
| ABORTED | 用户中止或不可恢复错误 |

兼容旧字段 `status`：`ONGOING` / `DONE`。

## 3. 迁移图

```
PREPARING ──► QUESTIONING ◄──┐
                 │           │
                 ▼           │
             EVALUATING ─────┘  （follow_up）
                 │
                 ▼ （finish / 达 maxQuestions / 主动 end）
             SUMMARIZING ──► COMPLETED

任意进行中 ──► ABORTED
```

## 4. 触发点

| 事件 | 迁移 |
| --- | --- |
| POST /start 建会话 | → PREPARING →（首题落库）→ QUESTIONING |
| POST /answer | QUESTIONING → EVALUATING → QUESTIONING 或 SUMMARIZING → COMPLETED |
| POST /end | QUESTIONING/EVALUATING → SUMMARIZING → COMPLETED |
| 评估 JSON action=finish | EVALUATING → SUMMARIZING → COMPLETED |
| WS RESUME | 不改状态，按 afterSeq 补发 SNAPSHOT |

## 5. 与提示词协作

评估提示词输出 JSON：`action` / `feedback` / `nextQuestion` / `score`。  
状态机以 `action` 与 `question_index >= max_questions` 共同决定是否收尾。
