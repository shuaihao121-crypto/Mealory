param(
    [Parameter(Mandatory = $true)]
    [string]$InputPath,
    [Parameter(Mandatory = $true)]
    [string]$OutputDirectory
)

$ErrorActionPreference = 'Stop'

$wordPath = 'C:\Program Files\Microsoft Office\root\Office16\WINWORD.EXE'
$pdftoppmPath = 'C:\Users\baizihao\.cache\codex-runtimes\codex-primary-runtime\dependencies\native\poppler\Library\bin\pdftoppm.exe'

if (-not (Test-Path -LiteralPath $wordPath)) {
    throw 'Microsoft Word is not installed at the expected path.'
}
if (-not (Test-Path -LiteralPath $pdftoppmPath)) {
    throw 'The bundled PDF rasterizer is missing.'
}

$resolvedInput = (Resolve-Path -LiteralPath $InputPath).Path
$resolvedOutput = [System.IO.Path]::GetFullPath($OutputDirectory)
New-Item -ItemType Directory -Force -Path $resolvedOutput | Out-Null

if (-not ('IsolatedWordNative' -as [type])) {
    Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
using System.Text;

public static class IsolatedWordNative
{
    private delegate bool EnumWindowsProc(IntPtr hwnd, IntPtr lParam);

    [DllImport("user32.dll")]
    private static extern bool EnumWindows(EnumWindowsProc callback, IntPtr lParam);

    [DllImport("user32.dll")]
    private static extern bool EnumChildWindows(IntPtr parent, EnumWindowsProc callback, IntPtr lParam);

    [DllImport("user32.dll")]
    private static extern uint GetWindowThreadProcessId(IntPtr hwnd, out uint processId);

    [DllImport("user32.dll", CharSet = CharSet.Unicode)]
    private static extern int GetClassName(IntPtr hwnd, StringBuilder className, int maxCount);

    [DllImport("oleacc.dll")]
    private static extern int AccessibleObjectFromWindow(
        IntPtr hwnd,
        uint objectId,
        ref Guid interfaceId,
        [MarshalAs(UnmanagedType.Interface)] out object result);

    public static object GetWordWindow(int processId)
    {
        IntPtr documentPane = IntPtr.Zero;
        EnumWindows((top, state) =>
        {
            uint owner;
            GetWindowThreadProcessId(top, out owner);
            if (owner != (uint)processId)
                return true;

            EnumChildWindows(top, (child, childState) =>
            {
                var name = new StringBuilder(256);
                GetClassName(child, name, name.Capacity);
                if (name.ToString() == "_WwG")
                {
                    documentPane = child;
                    return false;
                }
                return true;
            }, IntPtr.Zero);
            return documentPane == IntPtr.Zero;
        }, IntPtr.Zero);

        if (documentPane == IntPtr.Zero)
            return null;

        const uint OBJID_NATIVEOM = 0xFFFFFFF0;
        Guid iidDispatch = new Guid("00020400-0000-0000-C000-000000000046");
        object window;
        int result = AccessibleObjectFromWindow(documentPane, OBJID_NATIVEOM, ref iidDispatch, out window);
        if (result != 0)
            Marshal.ThrowExceptionForHR(result);
        return window;
    }
}
'@
}

$tempDirectory = Join-Path $resolvedOutput 'word-temp'
$tempDirectory = [System.IO.Path]::GetFullPath($tempDirectory)
if (-not $tempDirectory.StartsWith($resolvedOutput + [System.IO.Path]::DirectorySeparatorChar)) {
    throw 'Unexpected temporary directory path.'
}
New-Item -ItemType Directory -Force -Path $tempDirectory | Out-Null
$tempDocx = Join-Path $tempDirectory ([System.IO.Path]::GetFileName($resolvedInput))
Copy-Item -LiteralPath $resolvedInput -Destination $tempDocx -Force

$pdfPath = Join-Path $resolvedOutput 'rendered.pdf'
$process = $null
$wordWindow = $null
$application = $null
$document = $null

try {
    $quotedDocument = '"' + $tempDocx + '"'
    $process = Start-Process -FilePath $wordPath -ArgumentList @('/x', '/q', $quotedDocument) -PassThru -WindowStyle Hidden

    $deadline = [DateTime]::UtcNow.AddSeconds(30)
    while ([DateTime]::UtcNow -lt $deadline -and $null -eq $wordWindow) {
        Start-Sleep -Milliseconds 300
        if ($process.HasExited) {
            throw 'The isolated Word process exited before the document could be rendered.'
        }
        $wordWindow = [IsolatedWordNative]::GetWordWindow($process.Id)
    }
    if ($null -eq $wordWindow) {
        throw 'Could not connect to the isolated Word document window.'
    }

    $application = $wordWindow.Application
    $application.Visible = $false
    $application.DisplayAlerts = 0
    $document = $wordWindow.Document
    $document.ExportAsFixedFormat($pdfPath, 17)
    $document.Close($false)
    $document = $null
    $application.Quit()
    $application = $null

    & $pdftoppmPath -png -r 144 $pdfPath (Join-Path $resolvedOutput 'page')
    if ($LASTEXITCODE -ne 0) {
        throw "PDF rasterization failed with exit code $LASTEXITCODE."
    }
}
finally {
    if ($null -ne $document) {
        try { $document.Close($false) } catch {}
    }
    if ($null -ne $application) {
        try { $application.Quit() } catch {}
    }
    foreach ($item in @($document, $application, $wordWindow)) {
        if ($null -ne $item -and [System.Runtime.InteropServices.Marshal]::IsComObject($item)) {
            [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($item)
        }
    }
    if ($null -ne $process -and -not $process.HasExited) {
        Stop-Process -Id $process.Id -Force
    }
    Remove-Item -LiteralPath $tempDirectory -Recurse -Force -ErrorAction SilentlyContinue
}

$pageCount = (Get-ChildItem -LiteralPath $resolvedOutput -Filter 'page-*.png').Count
Write-Output "Rendered $pageCount pages to $resolvedOutput"
