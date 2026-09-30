param([ValidateSet('mysql-test','mysql-variants-test')][string]$Instance='mysql-test')
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
$state=Get-Content -LiteralPath (Join-Path $projectRoot "data\$Instance-state.json") -Raw | ConvertFrom-Json
if ($state.port -ne 3307 -or $state.database -ne 'techshop_test') { throw 'Unexpected test instance.' }
$configPath=Join-Path $projectRoot 'data\mysql-test-admin.cnf'
@("[client]","host=127.0.0.1","port=3307","user=root","password=$($state.rootPassword)") | Set-Content -LiteralPath $configPath -Encoding ASCII
try { & 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqladmin.exe' "--defaults-extra-file=$configPath" shutdown } finally { Remove-Item -LiteralPath $configPath }
