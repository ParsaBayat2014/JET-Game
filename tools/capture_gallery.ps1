param(
    [string]$OutputDirectory=(Join-Path (Split-Path -Parent $PSScriptRoot) 'releases\gallery-final'),
    [string]$AdbPath='C:\Program Files\BlueStacks_nxt\HD-Adb.exe',
    [string]$PlayerPath='C:\Program Files\BlueStacks_nxt\HD-Player.exe',
    [string]$Serial='emulator-5554',
    [int]$AdbPort=5039,
    [string]$Instance='Nougat32',
    [int]$FirstCaptureSeconds=9,
    [int]$SceneSeconds=25,
    [switch]$AlreadyLaunched
)
$ErrorActionPreference='Stop'
# Build :app:assembleQa with -PgalleryCapture=true before launching this isolated gallery.
if(-not (Test-Path -LiteralPath $AdbPath)){throw "ADB not found: $AdbPath"}
if($SceneSeconds -ne 25){throw 'GalleryActivity holds each scene for 25 seconds. Keep SceneSeconds=25.'}
if($FirstCaptureSeconds -lt 4 -or $FirstCaptureSeconds -gt 16){throw 'Capture offset must remain inside the first scene, between 4 and 16 seconds.'}
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$OutputDirectory=[IO.Path]::GetFullPath($OutputDirectory)
$names=@('01-jet','02-action-3d','03-guardian-turbo','04-together-3d','05-candy-25d','06-worlds-crew')
$descriptions=@('Brand and original pilot art','Real GLES2 action scene','Real GLES2 guardian and turbo','Real paired GLES2 shared-device action','Production Canvas perspective in Candy Cosmos','World assets, original characters and campaign composition')
$timer=[Diagnostics.Stopwatch]::StartNew()
if(-not $AlreadyLaunched){
    if(-not (Test-Path -LiteralPath $PlayerPath)){throw "BlueStacks player not found: $PlayerPath"}
    Start-Process -FilePath $PlayerPath -ArgumentList @('--instance',$Instance,'--cmd','launchApp','--package','com.neonrush.arcade.qa') -WindowStyle Hidden | Out-Null
}
$records=@()
function Save-NativeScreenshot([string]$Destination){
    $processInfo=[Diagnostics.ProcessStartInfo]::new()
    $processInfo.FileName=$AdbPath
    $processInfo.Arguments="-P $AdbPort -s $Serial exec-out screencap -p"
    $processInfo.UseShellExecute=$false
    $processInfo.CreateNoWindow=$true
    $processInfo.RedirectStandardOutput=$true
    $processInfo.RedirectStandardError=$true
    $process=[Diagnostics.Process]::Start($processInfo)
    $errorTask=$process.StandardError.ReadToEndAsync()
    $file=[IO.File]::Create($Destination)
    try{
        $copyTask=$process.StandardOutput.BaseStream.CopyToAsync($file)
        if(-not $process.WaitForExit(8000)){$process.Kill();$process.WaitForExit();throw 'Native ADB screenshot timed out.'}
        $copyTask.GetAwaiter().GetResult()
        $errors=$errorTask.GetAwaiter().GetResult()
        if($process.ExitCode -ne 0){throw "Native ADB screenshot failed: $errors"}
    }finally{$file.Dispose();$process.Dispose()}
    $png=[IO.File]::ReadAllBytes($Destination)
    if($png.Length -lt 20000 -or $png[0] -ne 137 -or $png[1] -ne 80 -or $png[2] -ne 78 -or $png[3] -ne 71){throw 'ADB did not return a complete PNG screenshot.'}
    if($png[$png.Length-8] -ne 73 -or $png[$png.Length-7] -ne 69 -or $png[$png.Length-6] -ne 78 -or $png[$png.Length-5] -ne 68){throw 'Native PNG is incomplete: missing IEND chunk.'}
    $width=($png[16]*16777216)+($png[17]*65536)+($png[18]*256)+$png[19]
    $height=($png[20]*16777216)+($png[21]*65536)+($png[22]*256)+$png[23]
    if($height -le $width){throw "Expected a full portrait Android capture; received $($width)x$($height)."}
    return @{width=$width;height=$height;bytes=$png.Length}
}
for($index=0;$index -lt $names.Length;$index++){
    $at=$FirstCaptureSeconds+$index*$SceneSeconds
    while($timer.Elapsed.TotalSeconds -lt $at){
        $remaining=$at-$timer.Elapsed.TotalSeconds
        Start-Sleep -Milliseconds ([Math]::Max(1,[Math]::Min(1000,[int]($remaining*1000))))
    }
    $destination=Join-Path $OutputDirectory ($names[$index]+'.png')
    $info=Save-NativeScreenshot $destination
    $hash=(Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash.ToLowerInvariant()
    $records+=@{scene=$index;file=($names[$index]+'.png');description=$descriptions[$index];width=$info.width;height=$info.height;bytes=$info.bytes;sha256=$hash;elapsedSeconds=[Math]::Round($timer.Elapsed.TotalSeconds,2)}
    Write-Output "Captured scene $index : $destination ($($info.width)x$($info.height))"
}
$manifest=@{format='JET native Android promotional gallery';package='com.neonrush.arcade.qa';method='adb exec-out screencap -p';curatedFixtures=$true;containsUnmodifiedProductionUi=$false;requiresVisualVerification=$true;scenes=$records}
[IO.File]::WriteAllText((Join-Path $OutputDirectory 'gallery.json'),($manifest | ConvertTo-Json -Depth 6),[Text.UTF8Encoding]::new($false))
Write-Output 'Six native PNGs captured. Inspect all six before delivery; timed capture does not itself verify scene identity or layout.'
