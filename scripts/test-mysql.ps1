param([ValidateSet('mysql-test','mysql-variants-test')][string]$Instance='mysql-test')
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
$state=Get-Content -LiteralPath (Join-Path $projectRoot "data\$Instance-state.json") -Raw | ConvertFrom-Json
if ($state.database -ne 'techshop_test' -or $state.port -ne 3307) { throw 'Refusing to run create-drop tests against an unexpected database.' }
$env:SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3307/techshop_test?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Asia/Ho_Chi_Minh'
$env:SPRING_DATASOURCE_USERNAME=$state.username
$env:SPRING_DATASOURCE_PASSWORD=$state.password
$env:SPRING_DATASOURCE_DRIVER_CLASS_NAME='com.mysql.cj.jdbc.Driver'
$env:SPRING_JPA_DATABASE_PLATFORM='org.hibernate.dialect.MySQLDialect'
$env:SPRING_FLYWAY_ENABLED='true'
$env:SPRING_JPA_HIBERNATE_DDL_AUTO='validate'
Push-Location $projectRoot
try { $ErrorActionPreference='Continue'; & .\mvnw.cmd -q '-Dtest=CommerceApiTests,AdminApiTests,PaymentApiTests' test; $testExit=$LASTEXITCODE } finally {
    Pop-Location
    'SPRING_DATASOURCE_URL','SPRING_DATASOURCE_USERNAME','SPRING_DATASOURCE_PASSWORD','SPRING_DATASOURCE_DRIVER_CLASS_NAME','SPRING_JPA_DATABASE_PLATFORM','SPRING_FLYWAY_ENABLED','SPRING_JPA_HIBERNATE_DDL_AUTO' | ForEach-Object { Remove-Item -LiteralPath "Env:$_" -ErrorAction SilentlyContinue }
}
exit $testExit
