param([Parameter(Mandatory=$true)][string]$Arguments,[string]$OutputFile='',[int]$TimeoutSeconds=10)
$ErrorActionPreference='Stop'
$info=New-Object System.Diagnostics.ProcessStartInfo
$info.FileName='C:\Program Files\BlueStacks_nxt\HD-Adb.exe'
$info.Arguments='-P 5039 -s emulator-5554 '+$Arguments
$info.UseShellExecute=$false
$info.CreateNoWindow=$true
$info.RedirectStandardOutput=$true
$info.RedirectStandardError=$true
$process=[System.Diagnostics.Process]::Start($info)
$errorTask=$process.StandardError.ReadToEndAsync()
if($OutputFile){
  $stream=[System.IO.File]::Create($OutputFile)
  $outputTask=$process.StandardOutput.BaseStream.CopyToAsync($stream)
}else{$outputTask=$process.StandardOutput.ReadToEndAsync()}
$timedOut=-not $process.WaitForExit($TimeoutSeconds*1000)
if($timedOut){$process.Kill();$process.WaitForExit()}
$result=$outputTask.GetAwaiter().GetResult()
if($OutputFile){$stream.Dispose();Get-Item -LiteralPath $OutputFile | Select-Object FullName,Length}
elseif($result){$result.Substring([Math]::Max(0,$result.Length-5000))}
$errors=$errorTask.GetAwaiter().GetResult()
if($errors){$errors.Substring([Math]::Max(0,$errors.Length-1000))}
if($timedOut){Write-Warning 'ADB command timed out and its client was stopped.'}