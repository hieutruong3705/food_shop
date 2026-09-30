param([switch]$ResumeInitialized, [switch]$RestartExisting, [ValidateSet('mysql-test','mysql-variants-test')][string]$Instance='mysql-test')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$mysqlBase = 'C:\Program Files\MySQL\MySQL Server 8.0'
$mysqlData = Join-Path $projectRoot "data\$Instance"
$mysqlState = Join-Path $projectRoot "data\$Instance-state.json"
function Get-TestListener {
    foreach ($listener in (Get-NetTCPConnection -LocalPort 3307 -State Listen -ErrorAction SilentlyContinue)) {
        $server = Get-CimInstance Win32_Process -Filter "ProcessId=$($listener.OwningProcess)"
        if ($server.Name -eq 'mysqld.exe' -and $server.CommandLine -and $server.CommandLine.Contains($mysqlData)) { return $listener }
    }
}
if (Test-Path -LiteralPath $mysqlState) {
    if (!$RestartExisting) { throw 'Test MySQL state exists. Use -RestartExisting to start it without initializing or changing data.' }
    $recorded=Get-Content -LiteralPath $mysqlState -Raw | ConvertFrom-Json
    if ($recorded.port -ne 3307 -or $recorded.database -ne 'techshop_test') { throw 'Unexpected test instance; refusing to start.' }
    if (Get-NetTCPConnection -LocalPort 3307 -State Listen -ErrorAction SilentlyContinue) { throw 'Port 3307 is already in use; reuse the running test instance.' }
    if (!(Test-Path -LiteralPath (Join-Path $mysqlData 'mysql.ibd'))) { throw 'Existing test database files are missing.' }
    $process=Start-Process -FilePath "$mysqlBase\bin\mysqld.exe" -ArgumentList @('--no-defaults',"--basedir=`"$mysqlBase`"","--datadir=`"$mysqlData`"",'--port=3307','--bind-address=127.0.0.1','--mysqlx=0',"--log-error=`"$mysqlData\server.log`"") -WindowStyle Hidden -PassThru
    for ($attempt=0; $attempt -lt 30; $attempt++) {
        $listener = Get-TestListener
        if ($listener) {
            $recorded.pid=$listener.OwningProcess
            $recorded | ConvertTo-Json | Set-Content -LiteralPath $mysqlState -Encoding UTF8
            Write-Output 'Existing isolated MySQL started on 127.0.0.1:3307. Data and credentials retained.'
            exit 0
        }
        if ($process.HasExited) { throw 'Test MySQL exited; inspect data/mysql-test/server.log.' }
        Start-Sleep -Milliseconds 500
    }
    throw 'Test MySQL did not become ready; inspect data/mysql-test/server.log.'
}
if ($RestartExisting) { throw 'No existing MySQL state to restart.' }
if (Get-NetTCPConnection -LocalPort 3307 -State Listen -ErrorAction SilentlyContinue) { throw 'Port 3307 is already in use.' }
New-Item -ItemType Directory -Path $mysqlData -Force | Out-Null
if (!$ResumeInitialized) {
    if (Get-ChildItem -LiteralPath $mysqlData -Force) { throw 'Test MySQL directory must be empty.' }
    $initialize=Start-Process -FilePath "$mysqlBase\bin\mysqld.exe" -ArgumentList @('--no-defaults','--initialize-insecure',"--basedir=`"$mysqlBase`"","--datadir=`"$mysqlData`"",'--console') -WindowStyle Hidden -Wait -PassThru -RedirectStandardError (Join-Path $projectRoot 'data\mysql-initialize.log')
    if ($initialize.ExitCode -ne 0) { throw 'Could not initialize isolated MySQL.' }
} elseif (!(Test-Path -LiteralPath (Join-Path $mysqlData 'mysql.ibd'))) { throw 'No initialized test instance found.' }
$mysqlProcess = Start-Process -FilePath "$mysqlBase\bin\mysqld.exe" -ArgumentList @('--no-defaults',"--basedir=`"$mysqlBase`"", "--datadir=`"$mysqlData`"",'--port=3307','--bind-address=127.0.0.1','--mysqlx=0',"--log-error=`"$mysqlData\server.log`"") -WindowStyle Hidden -PassThru
$ready = $false
for ($attempt=0; $attempt -lt 30; $attempt++) {
    $listener = Get-TestListener
    if ($listener) { $ready=$true; break }
    Start-Sleep -Milliseconds 500
}
if (!$ready) { throw 'Isolated MySQL did not start. Inspect data/mysql-test/server.log.' }
$testPassword = [guid]::NewGuid().ToString('N') + 'aA1'
$rootPassword = [guid]::NewGuid().ToString('N') + 'aA1'
$sql = "CREATE DATABASE techshop_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; CREATE USER 'techshop_test'@'localhost' IDENTIFIED BY '$testPassword'; GRANT ALL PRIVILEGES ON techshop_test.* TO 'techshop_test'@'localhost'; ALTER USER 'root'@'localhost' IDENTIFIED BY '$rootPassword';"
$sql | & "$mysqlBase\bin\mysql.exe" --no-defaults --host=127.0.0.1 --port=3307 --user=root
if ($LASTEXITCODE -ne 0) { throw 'Could not configure isolated test database.' }
@{pid=$listener.OwningProcess;port=3307;database='techshop_test';username='techshop_test';password=$testPassword;rootPassword=$rootPassword} | ConvertTo-Json | Set-Content -LiteralPath $mysqlState -Encoding UTF8
Write-Output "Isolated MySQL ready on 127.0.0.1:3307; credentials stored only in ignored data/$Instance-state.json."
