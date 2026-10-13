param([string]$Domain='techshop.work.gd')
# Packs what Git does not carry into data/cloud/techshop-cloud-data.tar.gz for compose.prod.yaml:
# .env, seed/mysql/techshop.sql, seed/app/uploads.tar.gz and config/application.properties.
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
$dataDir=Join-Path $projectRoot 'data'
$out=Join-Path $dataDir 'cloud'
$utf8=New-Object Text.UTF8Encoding $false
# Windows' own tar; a GNU tar earlier in PATH reads "D:\..." as a remote host.
$tar=Join-Path $env:SystemRoot 'System32\tar.exe'

$mysqlFile=Join-Path $dataDir 'mysql-local.properties'
if (!(Test-Path -LiteralPath $mysqlFile)) { throw 'data/mysql-local.properties not found: there is no local MySQL database to export.' }
$db=@{}
foreach ($line in Get-Content -LiteralPath $mysqlFile -Encoding UTF8) { if ($line -match '^\s*(DB_\w+)\s*=\s*(.*)$') { $db[$Matches[1]]=$Matches[2].Trim() } }
if ($db.DB_URL -match '^jdbc:mysql://([^:/?]+)(?::(\d+))?/([^?]+)') { $dbHost=$Matches[1]; $dbPort=if($Matches[2]){$Matches[2]}else{'3306'}; $dbName=$Matches[3] }
else { throw 'DB_URL in data/mysql-local.properties is not a jdbc:mysql:// address.' }
$mysqldump=(Get-Command mysqldump -ErrorAction SilentlyContinue).Source
if (!$mysqldump) { $mysqldump=Get-ChildItem 'C:\Program Files\MySQL\MySQL Server *\bin\mysqldump.exe' -ErrorAction SilentlyContinue | Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName }
if (!$mysqldump) { throw 'mysqldump not found. Add the bin folder of MySQL Server to PATH.' }

# .env is kept between runs: the MySQL volume on the server is created with these passwords.
foreach ($stale in 'seed','config','techshop-cloud-data.tar.gz') { Remove-Item -LiteralPath (Join-Path $out $stale) -Recurse -Force -ErrorAction SilentlyContinue }
New-Item -ItemType Directory -Force -Path (Join-Path $out 'seed\mysql'),(Join-Path $out 'seed\app'),(Join-Path $out 'config') | Out-Null

$dump=Join-Path $out 'seed\mysql\techshop.sql'
# The password goes through a client file, never the command line.
$clientFile=Join-Path $out 'mysql-client.cnf'
[IO.File]::WriteAllText($clientFile,"[client]`nhost=$dbHost`nport=$dbPort`nuser=$($db.DB_USERNAME)`npassword=`"$($db.DB_PASSWORD -replace '\\','\\' -replace '"','\"')`"`n",$utf8)
try {
    & $mysqldump "--defaults-extra-file=$clientFile" --single-transaction --no-tablespaces --set-gtid-purged=OFF --default-character-set=utf8mb4 "--result-file=$dump" $dbName
    if ($LASTEXITCODE -ne 0) { throw "mysqldump failed with exit code $LASTEXITCODE." }
} finally { Remove-Item -LiteralPath $clientFile -Force -ErrorAction SilentlyContinue }
Write-Output "Database $dbName -> seed/mysql/techshop.sql ($([math]::Round((Get-Item -LiteralPath $dump).Length/1MB,1)) MB)"

$uploads=Join-Path $dataDir 'uploads'
if (Test-Path -LiteralPath $uploads) {
    & $tar -czf (Join-Path $out 'seed\app\uploads.tar.gz') -C $uploads .
    if ($LASTEXITCODE -ne 0) { throw "tar failed on data/uploads with exit code $LASTEXITCODE." }
    Write-Output "Uploads: $(@(Get-ChildItem -LiteralPath $uploads -Recurse -File).Count) files -> seed/app/uploads.tar.gz"
} else { Write-Output 'Uploads: data/uploads not found, skipped.' }

# Same keys the local backend imports one by one; mysql-local stays here because the server has its own database.
$config=New-Object IO.MemoryStream
foreach ($name in 'chat','vnpay','mail','oauth') {
    $file=Join-Path $dataDir "$name-local.properties"
    if (!(Test-Path -LiteralPath $file)) { continue }
    $bytes=[IO.File]::ReadAllBytes($file)
    $skip=if($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF){3}else{0}
    $header=$utf8.GetBytes("# ---- data/$name-local.properties ----`n")
    $config.Write($header,0,$header.Length); $config.Write($bytes,$skip,$bytes.Length-$skip); $config.WriteByte(10)
    Write-Output "Config: data/$name-local.properties -> config/application.properties"
}
[IO.File]::WriteAllBytes((Join-Path $out 'config\application.properties'),$config.ToArray())

$envFile=Join-Path $out '.env'
if (!(Test-Path -LiteralPath $envFile)) {
    [IO.File]::WriteAllText($envFile,"COMPOSE_FILE=compose.prod.yaml`nDOMAIN=$Domain`nDB_PASSWORD=$([guid]::NewGuid().ToString('N'))`nMYSQL_ROOT_PASSWORD=$([guid]::NewGuid().ToString('N'))`n",$utf8)
    Write-Output "Created data/cloud/.env for $Domain with new database passwords."
} elseif ($PSBoundParameters.ContainsKey('Domain')) {
    [IO.File]::WriteAllText($envFile,(([IO.File]::ReadAllText($envFile)) -replace '(?m)^DOMAIN=.*$',"DOMAIN=$Domain"),$utf8)
    Write-Output "Kept the passwords in data/cloud/.env, DOMAIN set to $Domain."
} else { Write-Output 'Kept the existing data/cloud/.env.' }

$bundle=Join-Path $out 'techshop-cloud-data.tar.gz'
& $tar -czf $bundle -C $out .env seed config
if ($LASTEXITCODE -ne 0) { throw "tar failed on the bundle with exit code $LASTEXITCODE." }
Write-Output "Bundle: data/cloud/techshop-cloud-data.tar.gz ($([math]::Round((Get-Item -LiteralPath $bundle).Length/1MB,1)) MB). It holds passwords and customer data: keep it out of Git and chats."
Write-Output 'Next: copy it to the server, unpack it in the folder that has compose.prod.yaml, then run: docker compose up -d --build (see README).'
