param([ValidateSet('mysql-test','mysql-variants-test')][string]$Instance='mysql-test')
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
$state=Get-Content -LiteralPath (Join-Path $projectRoot "data\$Instance-state.json") -Raw | ConvertFrom-Json
if ($state.port -ne 3307 -or $state.database -ne 'techshop_test') { throw 'Unexpected isolated instance.' }
$schema='techshop_migration_'+[guid]::NewGuid().ToString('N').Substring(0,8)
$config=Join-Path $projectRoot 'data\mysql-migration-admin.cnf'
@('[client]','host=127.0.0.1','port=3307','user=root',"password=$($state.rootPassword)") | Set-Content -LiteralPath $config -Encoding ASCII
try { "CREATE DATABASE $schema CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" | & 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe' "--defaults-extra-file=$config";if($LASTEXITCODE -ne 0){throw 'Cannot create isolated migration schema.'} } finally { Remove-Item -LiteralPath $config }
$env:MIGRATION_TEST_URL="jdbc:mysql://127.0.0.1:3307/${schema}?allowPublicKeyRetrieval=true&useSSL=false"
$env:MIGRATION_TEST_USERNAME='root'
$env:MIGRATION_TEST_PASSWORD=$state.rootPassword
Push-Location $projectRoot
try { $ErrorActionPreference='Continue'; & .\mvnw.cmd -q '-Dtest=LegacyMigrationTest' test; $migrationExit=$LASTEXITCODE } finally { Pop-Location;Remove-Item Env:MIGRATION_TEST_URL,Env:MIGRATION_TEST_USERNAME,Env:MIGRATION_TEST_PASSWORD -ErrorAction SilentlyContinue }
exit $migrationExit
