# Deploy script for sync-backend

$SERVER = "root@61114-52681.pph-server.de"
$PORT = "54321"
$REMOTE_PATH = "/opt/sync-backend"
$LOCAL_PATH = $PSScriptRoot

Write-Host "Deploying sync-backend to $SERVER on port $PORT..." -ForegroundColor Cyan

# Step 1: Stop containers on server
Write-Host "`n[1/4] Stopping containers..." -ForegroundColor Yellow
ssh -p $PORT $SERVER "cd $REMOTE_PATH && docker compose down || true"

# Step 2: Clean remote folder
Write-Host "`n[2/4] Cleaning remote folder..." -ForegroundColor Yellow
ssh -p $PORT $SERVER "rm -rf $REMOTE_PATH"
ssh -p $PORT $SERVER "mkdir -p $REMOTE_PATH"
ssh -p $PORT $SERVER "cp /opt/sync-backend.env $REMOTE_PATH/.env"

# Step 3: Upload files
Write-Host "`n[3/4] Uploading files..." -ForegroundColor Yellow
scp -P $PORT -r `
    "$LOCAL_PATH\src" `
    "$LOCAL_PATH\gradle" `
    "$LOCAL_PATH\build.gradle.kts" `
    "$LOCAL_PATH\settings.gradle.kts" `
    "$LOCAL_PATH\gradle.properties" `
    "$LOCAL_PATH\gradlew" `
    "$LOCAL_PATH\gradlew.bat" `
    "$LOCAL_PATH\Dockerfile" `
    "$LOCAL_PATH\docker-compose.yml" `
    "$LOCAL_PATH\Caddyfile" `
    "${SERVER}:${REMOTE_PATH}/"

# Step 4: Build and start containers
Write-Host "`n[4/4] Building and starting containers..." -ForegroundColor Yellow
ssh -p $PORT $SERVER "cd $REMOTE_PATH && docker compose up -d --build"

Write-Host "`nDeployment complete!" -ForegroundColor Green

# Show logs
Write-Host "Streaming logs... (Press Ctrl+C to exit)" -ForegroundColor Gray
ssh -p $PORT $SERVER "cd $REMOTE_PATH && docker compose logs -f"