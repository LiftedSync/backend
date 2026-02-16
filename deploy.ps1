# Deploy script for sync-backend

$SERVER = "root@61114-52681.pph-server.de"
$PORT = "54321"
$REMOTE_PATH = "/opt/sync-backend"
$LOCAL_PATH = $PSScriptRoot

Write-Host "Deploying sync-backend to $SERVER" -ForegroundColor Cyan

# Step 1: Build fat JAR locally
Write-Host "`n[1/5] Building fat JAR locally..." -ForegroundColor Yellow
& "$LOCAL_PATH\gradlew.bat" -p "$LOCAL_PATH" clean buildFatJar
if ($LASTEXITCODE -ne 0) { Write-Host "Build failed!" -ForegroundColor Red; exit 1 }

# Step 2: Stop containers on server
Write-Host "`n[2/5] Stopping containers..." -ForegroundColor Yellow
ssh -p $PORT $SERVER "cd $REMOTE_PATH && docker compose down || true"

# Step 3: Clean remote folder
Write-Host "`n[3/5] Cleaning remote folder..." -ForegroundColor Yellow
ssh -p $PORT $SERVER "rm -rf $REMOTE_PATH"
ssh -p $PORT $SERVER "mkdir -p $REMOTE_PATH"
ssh -p $PORT $SERVER "cp /opt/sync-backend.env $REMOTE_PATH/.env"

# Step 4: Upload files
Write-Host "`n[4/5] Uploading files..." -ForegroundColor Yellow
scp -P $PORT `
    "$LOCAL_PATH\build\libs\sync-backend-all.jar" `
    "${SERVER}:${REMOTE_PATH}/app.jar"
scp -P $PORT `
    "$LOCAL_PATH\Dockerfile" `
    "$LOCAL_PATH\Caddyfile" `
    "$LOCAL_PATH\docker-compose.yml" `
    "${SERVER}:${REMOTE_PATH}/"

# Step 5: Build and start containers
Write-Host "`n[5/5] Building image and starting containers..." -ForegroundColor Yellow
ssh -p $PORT $SERVER "cd $REMOTE_PATH && docker compose up -d --build"

Write-Host "`nDeployment complete!" -ForegroundColor Green

# Show logs
Write-Host "Streaming logs... (Press Ctrl+C to exit)" -ForegroundColor Gray
ssh -p $PORT $SERVER "cd $REMOTE_PATH && docker compose logs -f"
