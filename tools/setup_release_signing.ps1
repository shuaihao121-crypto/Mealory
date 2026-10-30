param(
    [string]$JavaHome = 'C:\Program Files\Android\Android Studio\jbr'
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$metadataPath = Join-Path $root 'release.properties'
$signingDirectory = Join-Path $root 'release-signing'
$keystorePath = Join-Path $signingDirectory 'mealory-release.jks'
$propertiesPath = Join-Path $root 'keystore.properties'
$keytool = Join-Path $JavaHome 'bin\keytool.exe'
$alias = 'mealory'

if (-not (Test-Path -LiteralPath $metadataPath -PathType Leaf)) {
    throw 'Missing release.properties. Fill the public release metadata first.'
}
if (-not (Test-Path -LiteralPath $keytool -PathType Leaf)) {
    throw "keytool not found: $keytool"
}

$metadata = ConvertFrom-StringData (
    (Get-Content -LiteralPath $metadataPath -Encoding UTF8) |
        Where-Object { $_ -notmatch '^\s*#' -and $_ -match '=' } |
        Out-String
)
$developerName = [string]$metadata.developerName
if ([string]::IsNullOrWhiteSpace($developerName)) {
    throw 'release.properties must contain developerName.'
}

New-Item -ItemType Directory -Path $signingDirectory -Force | Out-Null
if (-not (Test-Path -LiteralPath $keystorePath -PathType Leaf)) {
    Write-Host 'Create the permanent Mealory release key.'
    Write-Host 'Enter a strong password when keytool asks. Keep it outside this repository.'
    & $keytool -genkeypair `
        -keystore $keystorePath `
        -storetype PKCS12 `
        -alias $alias `
        -keyalg RSA `
        -keysize 4096 `
        -validity 10000 `
        -dname "CN=$($developerName.Trim()), O=Mealory, C=CN"
    if ($LASTEXITCODE -ne 0) {
        throw "keytool failed with exit code $LASTEXITCODE."
    }
} else {
    Write-Host "Using existing keystore: $keystorePath"
}

$password = Read-Host 'Re-enter the keystore password to configure the local build' -AsSecureString
$passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($password)
try {
    $plainPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    if ([string]::IsNullOrWhiteSpace($plainPassword)) {
        throw 'The keystore password cannot be empty.'
    }
    $configuredKeystorePath = $keystorePath.Replace('\', '/')
    $content = @(
        "storeFile=$configuredKeystorePath"
        "storePassword=$plainPassword"
        "keyAlias=$alias"
        "keyPassword=$plainPassword"
    )
    Set-Content -LiteralPath $propertiesPath -Value $content -Encoding UTF8
} finally {
    if ($passwordPointer -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    }
    $plainPassword = $null
}

Write-Output "Release keystore: $keystorePath"
Write-Output "Local signing configuration: $propertiesPath"
Write-Output 'Back up the keystore and password in at least two secure locations before publishing.'
