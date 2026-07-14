@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion
echo.
echo ==================== AI Resume Interview 快速启动脚本 ====================
echo.

cd /d "%~dp0"

REM ---- 加载 .env 到当前环境（后端启动必需）----
if exist ".env" (
  echo [0/3] 加载 .env ...
  for /f "usebackq eol=# tokens=1,* delims==" %%a in (".env") do (
    if not "%%a"=="" (
      set "%%a=%%b"
    )
  )
) else (
  echo 警告: 未找到 .env，将使用默认数据库账号
  if not defined MYSQL_USER set MYSQL_USER=resume
  if not defined MYSQL_PASSWORD set MYSQL_PASSWORD=change-me-resume
  if not defined MYSQL_DATABASE set MYSQL_DATABASE=resume_assistant
)

echo [1/3] 启动 MySQL 和 Redis (Docker)
echo -----------------------------------

echo 检查 Docker 是否运行...
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误: Docker Desktop 未运行! 请先启动 Docker Desktop。
    pause
    exit /b 1
)

if not exist docker-compose.yml (
    echo 错误: docker-compose.yml 文件不存在!
    pause
    exit /b 1
)

echo 正在启动 MySQL 和 Redis 容器...
docker compose up -d mysql redis
if %errorlevel% neq 0 (
    echo 尝试使用 docker-compose 命令...
    docker-compose up -d mysql redis
    if %errorlevel% neq 0 (
        echo 错误: Docker 启动失败!
        pause
        exit /b 1
    )
)

echo.
echo 等待 MySQL 和 Redis 启动完成...
set MAX_RETRY=30
set RETRY=0
:CHECK_HEALTH
set /a RETRY=%RETRY%+1
if %RETRY% gtr %MAX_RETRY% (
    echo 超时: MySQL 或 Redis 启动失败!
    pause
    exit /b 1
)

docker inspect --format "{{.State.Health.Status}}" resume-assistant-mysql 2>&1 | findstr "healthy" >nul
if %errorlevel% neq 0 (
    timeout /t 2 /nobreak >nul
    goto CHECK_HEALTH
)

docker inspect --format "{{.State.Health.Status}}" resume-assistant-redis 2>&1 | findstr "healthy" >nul
if %errorlevel% neq 0 (
    timeout /t 2 /nobreak >nul
    goto CHECK_HEALTH
)

echo.
echo [2/3] 启动后端服务 (Spring Boot)
echo -----------------------------------
cd /d "%~dp0server"
if not exist pom.xml (
    echo 错误: server/pom.xml 文件不存在!
    pause
    exit /b 1
)

REM 释放 8080，避免 "Port already in use" / exit code 1
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":8080" ^| findstr LISTENING') do (
  echo 发现 8080 已被占用 PID=%%p，正在结束旧进程...
  taskkill /F /PID %%p >nul 2>&1
)
timeout /t 2 /nobreak >nul

echo 正在启动后端服务 (dev 环境)...
REM 把 .env 变量传入新窗口
start "后端服务" /d "%~dp0server" cmd /k "chcp 65001>nul & set MYSQL_USER=%MYSQL_USER%& set MYSQL_PASSWORD=%MYSQL_PASSWORD%& set MYSQL_DATABASE=%MYSQL_DATABASE%& set MYSQL_ROOT_PASSWORD=%MYSQL_ROOT_PASSWORD%& set JWT_SECRET=%JWT_SECRET%& set DASHSCOPE_API_KEY=%DASHSCOPE_API_KEY%& set QWEN_API_KEY=%QWEN_API_KEY%& set ARK_API_KEY=%ARK_API_KEY%& set EMAIL_USERNAME=%EMAIL_USERNAME%& set EMAIL_PASSWORD=%EMAIL_PASSWORD%& set REDIS_PASSWORD=%REDIS_PASSWORD%& mvn spring-boot:run -Dspring-boot.run.profiles=dev -DskipTests"

echo.
echo [3/3] 启动前端服务 (Vue + Vite)
echo -----------------------------------
cd /d "%~dp0web"
if not exist package.json (
    echo 错误: web/package.json 文件不存在!
    pause
    exit /b 1
)

if not exist node_modules (
    echo 正在安装前端依赖...
    call npm install
    if %errorlevel% neq 0 (
        echo 警告: 前端依赖安装失败，尝试直接启动...
    )
) else (
    echo 前端依赖已存在，跳过安装...
)

for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":5173" ^| findstr LISTENING') do (
  echo 发现 5173 已被占用 PID=%%p，正在结束旧进程...
  taskkill /F /PID %%p >nul 2>&1
)

echo 正在启动前端服务...
start "前端服务" /d "%~dp0web" cmd /k "chcp 65001>nul & npm run dev"

echo.
echo ==================== 启动完成 ====================
echo.
echo 服务访问地址:
echo   - 前端: http://localhost:5173
echo   - 后端 API: http://localhost:8080
echo   - MySQL: localhost:3306
echo   - Redis: localhost:6379
echo.
echo 若后端窗口立刻报错退出，请把该窗口完整日志贴给我。
echo.
pause >nul