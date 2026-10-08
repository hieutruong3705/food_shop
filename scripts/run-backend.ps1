param([ValidateSet('mysql','h2')][string]$Database='mysql', [switch]$Packaged)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
$credentialFile=Join-Path $projectRoot 'data\dev-credentials.json'
if (!(Test-Path -LiteralPath $credentialFile)) {
    New-Item -ItemType Directory -Path (Join-Path $projectRoot 'data') -Force | Out-Null
    @{admin=([guid]::NewGuid().ToString()+'aA1!');user=([guid]::NewGuid().ToString()+'aA1!')} | ConvertTo-Json | Set-Content -LiteralPath $credentialFile -Encoding UTF8
}
$credentials=Get-Content -LiteralPath $credentialFile -Raw | ConvertFrom-Json
$env:DEMO_ADMIN_PASSWORD=$credentials.admin
$env:DEMO_USER_PASSWORD=$credentials.user
Write-Output 'Demo accounts: admin@techshop.test / user@techshop.test. Passwords are in data/dev-credentials.json (local, ignored by Git).'
$vnpayFile=Join-Path $projectRoot 'data/vnpay-local.properties'
if (Test-Path -LiteralPath $vnpayFile) { Write-Output 'VNPay: data/vnpay-local.properties found, Spring imports it automatically (local, ignored by Git).' }
else { Write-Output 'VNPay: disabled. Create data/vnpay-local.properties with shop.vnpay.enabled, shop.vnpay.tmn-code and shop.vnpay.secret to switch the sandbox on. See README.' }
$profiles=if($Database -eq 'mysql'){'dev,mysql-local'}else{'dev'}
if ($Database -eq 'mysql' -and !(Test-Path -LiteralPath (Join-Path $projectRoot 'data\mysql-local.properties'))) { throw 'Create data/mysql-local.properties with DB_URL, DB_USERNAME and DB_PASSWORD first. See docs/DATABASE.md.' }
# mvnw forks the application into its own JVM, so closing the terminal can leave it holding the port.
$port=if($env:PORT){[int]$env:PORT}else{8080}
foreach ($owner in @(Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique)) {
    $command=(Get-CimInstance Win32_Process -Filter "ProcessId = $owner" -ErrorAction SilentlyContinue).CommandLine
    if ($command -match 'com\.example\.shop\.ShopApplication|techshop\.jar') {
        Write-Output "Stopping the previous backend still holding port $port (PID $owner)."
        Stop-Process -Id $owner -Force -ErrorAction SilentlyContinue
    } else {
        throw "Port $port is held by PID $owner, which is not a TechShop backend. Stop it yourself first."
    }
}
for ($waited=0; $waited -lt 20 -and (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue); $waited++) { Start-Sleep -Milliseconds 250 }
Push-Location $projectRoot
try {
    $ErrorActionPreference='Continue'
    if($Packaged) { & java -jar target/techshop.jar "--spring.profiles.active=$profiles" --server.address=127.0.0.1 }
    else { & .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=$profiles" '-Dspring-boot.run.jvmArguments=-Dspring.devtools.restart.enabled=false' }
    $backendExit=$LASTEXITCODE
} finally { Pop-Location; Remove-Item Env:DEMO_ADMIN_PASSWORD,Env:DEMO_USER_PASSWORD -ErrorAction SilentlyContinue }
exit $backendExit
