#requires -Version 7.0
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$ValidationDatabasePrefix = 'ai_medical_care_release_validation_'
$ValidationDatabasePattern = '^' + [regex]::Escape($ValidationDatabasePrefix) + '\d{8}_\d{6}_[0-9a-f]{8}$'
$RunTimestamp = [DateTime]::UtcNow.ToString('yyyyMMdd_HHmmss')
$RunId = $RunTimestamp + '_' + ([guid]::NewGuid().ToString('N').Substring(0, 8))
$DatabaseName = $ValidationDatabasePrefix + $RunId
$EvidenceDirectory = Join-Path $ProjectRoot 'docs/verification/runs/'
$EvidencePath = Join-Path $EvidenceDirectory ("release-validation_{0}.md" -f $RunId)

function Get-OptionalEnvironmentValue {
    param([Parameter(Mandatory)][string]$Name, [Parameter(Mandatory)][string]$Default)
    $value = [Environment]::GetEnvironmentVariable($Name)
    if ([string]::IsNullOrWhiteSpace($value)) { return $Default }
    return $value.Trim()
}

function Get-RequiredEnvironmentValue {
    param([Parameter(Mandatory)][string]$Name)
    $value = [Environment]::GetEnvironmentVariable($Name)
    if ([string]::IsNullOrWhiteSpace($value)) { throw "$Name is required" }
    return $value
}

function Assert-ValidationDatabaseName {
    param([Parameter(Mandatory)][string]$DatabaseName)
    if ([string]::IsNullOrWhiteSpace($DatabaseName) -or $DatabaseName -cnotmatch $ValidationDatabasePattern) {
        throw 'Refusing to operate on a database outside the release-validation namespace'
    }
}

function Invoke-ChildProcess {
    param(
        [Parameter(Mandatory)][string]$FilePath,
        [Parameter(Mandatory)][string[]]$Arguments,
        [Parameter(Mandatory)][hashtable]$Environment,
        [string]$InputText
    )

    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $isCommandScript = $IsWindows -and [IO.Path]::GetExtension($FilePath) -in @('.cmd', '.bat')
    $startInfo.FileName = if ($isCommandScript) { $env:ComSpec } else { $FilePath }
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardInput = $null -ne $InputText
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    if ($isCommandScript) {
        [void]$startInfo.ArgumentList.Add('/d')
        [void]$startInfo.ArgumentList.Add('/s')
        [void]$startInfo.ArgumentList.Add('/c')
        [void]$startInfo.ArgumentList.Add($FilePath)
    }
    foreach ($argument in $Arguments) { [void]$startInfo.ArgumentList.Add($argument) }
    $sensitiveEnvironmentPattern = '(?i)(password|secret|token|api[_-]?key|jwt)'
    $inheritedSensitiveNames = @($startInfo.Environment.Keys | Where-Object { $_ -match $sensitiveEnvironmentPattern })
    foreach ($name in $inheritedSensitiveNames) { [void]$startInfo.Environment.Remove($name) }
    foreach ($entry in $Environment.GetEnumerator()) {
        $startInfo.Environment[$entry.Key] = [string]$entry.Value
    }

    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $startInfo
    [void]$process.Start()
    $stdoutTask = $process.StandardOutput.ReadToEndAsync()
    $stderrTask = $process.StandardError.ReadToEndAsync()
    if ($null -ne $InputText) {
        $process.StandardInput.Write($InputText)
        $process.StandardInput.Close()
    }
    $process.WaitForExit()
    $stdout = $stdoutTask.GetAwaiter().GetResult()
    $null = $stderrTask.GetAwaiter().GetResult()
    if ($process.ExitCode -ne 0) {
        throw "Child process failed with exit code $($process.ExitCode): $FilePath"
    }
    return $stdout.Trim()
}

$DbHost = Get-OptionalEnvironmentValue -Name 'RELEASE_VALIDATION_DB_HOST' -Default '127.0.0.1'
$DbPort = Get-OptionalEnvironmentValue -Name 'RELEASE_VALIDATION_DB_PORT' -Default '3306'
$DbUser = Get-OptionalEnvironmentValue -Name 'RELEASE_VALIDATION_DB_USER' -Default 'root'
$DbPassword = Get-RequiredEnvironmentValue -Name 'RELEASE_VALIDATION_DB_PASSWORD'
$RedisHost = Get-OptionalEnvironmentValue -Name 'RELEASE_VALIDATION_REDIS_HOST' -Default '127.0.0.1'
$RedisPort = Get-OptionalEnvironmentValue -Name 'RELEASE_VALIDATION_REDIS_PORT' -Default '6379'
$MysqlExecutable = Get-OptionalEnvironmentValue -Name 'RELEASE_VALIDATION_MYSQL_BIN' -Default 'mysql'
$MavenDefault = if ($IsWindows) { 'mvn.cmd' } else { 'mvn' }
$MavenExecutable = Get-OptionalEnvironmentValue -Name 'RELEASE_VALIDATION_MAVEN_BIN' -Default $MavenDefault
$JdbcUrl = "jdbc:mysql://$DbHost`:$DbPort/$DatabaseName?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$MysqlEnvironment = @{ MYSQL_PWD = $DbPassword }
$MavenEnvironment = @{
    MYSQL_PWD = $DbPassword
    RELEASE_VALIDATION_JDBC_URL = $JdbcUrl
    RELEASE_VALIDATION_DB_USER = $DbUser
    RELEASE_VALIDATION_DB_PASSWORD = $DbPassword
    RELEASE_VALIDATION_REDIS_HOST = $RedisHost
    RELEASE_VALIDATION_REDIS_PORT = $RedisPort
}
$DatabaseCreated = $false
$DatabaseCreateAttempted = $false
$DatabaseDropped = $false
$Status = 'PASS'
$FailureMessage = $null
$IndexStatus = 'not checked'

function Invoke-MySqlSql {
    param([Parameter(Mandatory)][string]$Sql, [string]$Database)
    $arguments = @('--batch', '--skip-column-names', '--host', $DbHost, '--port', $DbPort, '--user', $DbUser)
    if (-not [string]::IsNullOrWhiteSpace($Database)) { $arguments += @('--database', $Database) }
    return Invoke-ChildProcess -FilePath $MysqlExecutable -Arguments $arguments -Environment $MysqlEnvironment -InputText $Sql
}

function Get-SanitizedFailureMessage {
    param([Parameter(Mandatory)][string]$Message)
    $sanitized = if ([string]::IsNullOrEmpty($DbPassword)) { $Message } else { $Message.Replace($DbPassword, '[REDACTED]') }
    if ($sanitized.Length -gt 1000) { return $sanitized.Substring(0, 1000) + '...' }
    return $sanitized
}

try {
    Assert-ValidationDatabaseName -DatabaseName $DatabaseName
    $createSql = 'CREATE DATABASE `' + $DatabaseName + '` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;'
    $DatabaseCreateAttempted = $true
    Invoke-MySqlSql -Sql $createSql | Out-Null
    $DatabaseCreated = $true

    $baselinePath = Join-Path $ProjectRoot 'scripts/release-validation/schema-baseline.sql'
    Invoke-MySqlSql -Database $DatabaseName -Sql (Get-Content -Raw $baselinePath) | Out-Null

    $migrationFiles = @(
        'V2__secure_appointments.sql',
        'V3__roles_and_doctor_accounts.sql',
        'V4__appointment_lifecycle.sql',
        'V5__triage_cases.sql'
    )
    for ($Pass = 1; $Pass -le 2; $Pass++) {
        foreach ($migrationFile in $migrationFiles) {
            $migrationPath = Join-Path $ProjectRoot ('src/main/resources/db/migration/' + $migrationFile)
            Invoke-MySqlSql -Database $DatabaseName -Sql (Get-Content -Raw $migrationPath) | Out-Null
        }
    }

    $expectedIndexes = @(
        'uk_appointment_user_schedule', 'idx_appointment_user_id', 'idx_appointment_schedule_id',
        'uk_doctor_user_id', 'idx_appointment_doctor_status', 'idx_appointment_user_status',
        'idx_triage_case_patient_created', 'uk_triage_evidence_case_rank'
    )
    $indexSql = @"
SELECT index_name
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND index_name IN ('$($expectedIndexes -join "','")')
GROUP BY index_name
ORDER BY index_name;
"@
    $actualIndexOutput = Invoke-MySqlSql -Database $DatabaseName -Sql $indexSql
    $actualIndexes = @($actualIndexOutput -split '\r?\n') | Where-Object { $_ }
    $missingIndexes = $expectedIndexes | Where-Object { $_ -notin $actualIndexes }
    if ($missingIndexes.Count -gt 0) { throw ('Missing expected MySQL indexes: ' + ($missingIndexes -join ', ')) }
    $IndexStatus = "verified $($expectedIndexes.Count) indexes"

    Invoke-ChildProcess -FilePath $MavenExecutable -Arguments @('-Pexternal-integration-tests', '-Dtest=com.Liang.java.ai.langchain4j.release.*ExternalIntegrationTest', 'test') -Environment $MavenEnvironment | Out-Null
}
catch {
    $Status = 'FAIL'
    $FailureMessage = Get-SanitizedFailureMessage -Message $_.Exception.Message
}
finally {
    if ($DatabaseCreateAttempted) {
        try {
            Assert-ValidationDatabaseName -DatabaseName $DatabaseName
            $dropSql = 'DROP DATABASE IF EXISTS `' + $DatabaseName + '`;'
            Invoke-MySqlSql -Sql $dropSql | Out-Null
            $DatabaseDropped = $true
        }
        catch {
            $Status = 'FAIL'
            if ([string]::IsNullOrWhiteSpace($FailureMessage)) {
                $FailureMessage = Get-SanitizedFailureMessage -Message $_.Exception.Message
            }
        }
    }

    New-Item -ItemType Directory -Path $EvidenceDirectory -Force | Out-Null
    $evidence = @(
        '# Release Validation'
        ''
        "- Status: **$Status**"
        "- Timestamp (UTC): $RunTimestamp"
        ('- Temporary database: `{0}`' -f $DatabaseName)
        "- Database cleanup: $(if ($DatabaseDropped) { 'dropped' } else { 'not applicable or failed' })"
        "- Migration passes: V2 through V5, 2 passes"
        "- MySQL indexes: $IndexStatus"
        '- External tests: Maven profile `external-integration-tests`'
    )
    if (-not [string]::IsNullOrWhiteSpace($FailureMessage)) {
        $evidence += "- Failure: $FailureMessage"
    }
    Set-Content -Path $EvidencePath -Value ($evidence -join [Environment]::NewLine) -Encoding utf8
}

if ($Status -ne 'PASS') { throw $FailureMessage }
Write-Output "Release validation passed; evidence: $EvidencePath"
