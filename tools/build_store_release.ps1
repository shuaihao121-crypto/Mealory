param(
    [string]$JavaHome = 'C:\Program Files\Android\Android Studio\jbr'
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$gradle = Join-Path $root 'gradlew.bat'
$apkSource = Join-Path $root 'app\build\outputs\apk\release\app-release.apk'
$outputDirectory = Join-Path $root 'release-output\apk'
$apkDestination = Join-Path $outputDirectory 'Mealory-1.0.0-release.apk'
$reportPath = Join-Path $outputDirectory 'Mealory-1.0.0-release-verification.txt'
$sdkRoot = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$apksigner = Join-Path $sdkRoot 'build-tools\37.0.0\apksigner.bat'

foreach ($required in @('release.properties', 'keystore.properties')) {
    if (-not (Test-Path -LiteralPath (Join-Path $root $required) -PathType Leaf)) {
        throw "Missing $required. Complete the release metadata and signing setup first."
    }
}
if (-not (Test-Path -LiteralPath $gradle -PathType Leaf)) {
    throw 'Gradle wrapper is missing.'
}
if (-not (Test-Path -LiteralPath $JavaHome -PathType Container)) {
    throw "Java runtime not found: $JavaHome"
}
if (-not (Test-Path -LiteralPath $apksigner -PathType Leaf)) {
    throw "APK signer not found: $apksigner"
}

$env:JAVA_HOME = $JavaHome
& $gradle testDebugUnitTest lintDebug assembleRelease
if ($LASTEXITCODE -ne 0) {
    throw "Release build failed with exit code $LASTEXITCODE."
}
if (-not (Test-Path -LiteralPath $apkSource -PathType Leaf)) {
    throw 'Release APK was not produced at the expected path.'
}

New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
Copy-Item -LiteralPath $apkSource -Destination $apkDestination -Force

$verification = & $apksigner verify --verbose --print-certs $apkDestination 2>&1
if ($LASTEXITCODE -ne 0) {
    throw "APK signature verification failed.`n$($verification -join [Environment]::NewLine)"
}
$hash = (Get-FileHash -LiteralPath $apkDestination -Algorithm SHA256).Hash
$size = (Get-Item -LiteralPath $apkDestination).Length
$report = @(
    'Mealory 1.0.0 release verification'
    "Generated: $([DateTimeOffset]::Now.ToString('yyyy-MM-dd HH:mm:ss zzz'))"
    "File: $apkDestination"
    "Size: $size bytes"
    "SHA-256: $hash"
    ''
    'apksigner output:'
    $verification
)
Set-Content -LiteralPath $reportPath -Value $report -Encoding UTF8

Write-Output "Release APK: $apkDestination"
Write-Output "SHA-256: $hash"
Write-Output "Verification report: $reportPath"
