@echo off
chcp 65001 >nul
echo.
echo ==================== AI Resume Interview 快速启动脚本 ====================
echo.

echo [1/3] 启动 MySQL 和 Redis (Docker)
echo -----------------------------------
cd /d "%~dp0"

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

echo 正在启动后端服务 (dev 环境)...
start "后端服务" /d "%~dp0server" cmd /k "mvn clean spring-boot:run -Dspring-boot.run.profiles=dev"

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

echo 正在启动前端服务...
start "前端服务" /d "%~dp0web" cmd /k "npm run dev"

echo.
echo ==================== 启动完成 ====================
echo.
echo 服务访问地址:
echo   - 前端: http://localhost:5173
echo   - 后端 API: http://localhost:8080
echo   - MySQL: localhost:3306
echo   - Redis: localhost:6379
echo.
echo 按任意键关闭此窗口...
pause >nul