param(
    [string]$EffectiveDate = (Get-Date -Format 'yyyy年MM月dd日')
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$metadataPath = Join-Path $root 'release.properties'
$templatePath = Join-Path $root 'store\oppo\privacy-policy-template.html'
$outputDirectory = Join-Path $root 'release-output\privacy'
$outputPath = Join-Path $outputDirectory 'index.html'

if (-not (Test-Path -LiteralPath $metadataPath -PathType Leaf)) {
    throw 'Missing release.properties. Copy release.properties.example and fill the public metadata first.'
}
if (-not (Test-Path -LiteralPath $templatePath -PathType Leaf)) {
    throw 'Missing OPPO privacy policy template.'
}

$metadata = ConvertFrom-StringData (
    (Get-Content -LiteralPath $metadataPath -Encoding UTF8) |
        Where-Object { $_ -notmatch '^\s*#' -and $_ -match '=' } |
        Out-String
)
$developerName = [string]$metadata.developerName
$supportEmail = [string]$metadata.supportEmail

if ([string]::IsNullOrWhiteSpace($developerName)) {
    throw 'release.properties must contain developerName.'
}
if ($supportEmail -notmatch '^[^@\s]+@[^@\s]+\.[^@\s]+$') {
    throw 'release.properties must contain a valid supportEmail.'
}

$html = Get-Content -LiteralPath $templatePath -Raw -Encoding UTF8
$html = $html.Replace('{{EFFECTIVE_DATE}}', [System.Net.WebUtility]::HtmlEncode($EffectiveDate))
$html = $html.Replace('{{DEVELOPER_NAME}}', [System.Net.WebUtility]::HtmlEncode($developerName.Trim()))
$html = $html.Replace('{{SUPPORT_EMAIL}}', [System.Net.WebUtility]::HtmlEncode($supportEmail.Trim()))

New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
Set-Content -LiteralPath $outputPath -Value $html -Encoding UTF8
Write-Output "Privacy policy generated: $outputPath"
