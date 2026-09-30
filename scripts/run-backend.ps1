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
$profiles=if($Database -eq 'mysql'){'dev,mysql-local'}else{'dev'}
if ($Database -eq 'mysql' -and !(Test-Path -LiteralPath (Join-Path $projectRoot 'data\mysql-local.properties'))) { throw 'Create data/mysql-local.properties with DB_URL, DB_USERNAME and DB_PASSWORD first. See docs/DATABASE.md.' }
Push-Location $projectRoot
try {
    $ErrorActionPreference='Continue'
    if($Packaged) { & java -jar target/techshop.jar "--spring.profiles.active=$profiles" --server.address=127.0.0.1 }
    else { & .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=$profiles" '-Dspring-boot.run.jvmArguments=-Dspring.devtools.restart.enabled=false' }
    $backendExit=$LASTEXITCODE
} finally { Pop-Location; Remove-Item Env:DEMO_ADMIN_PASSWORD,Env:DEMO_USER_PASSWORD -ErrorAction SilentlyContinue }
exit $backendExit
