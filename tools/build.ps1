param(
    [ValidateSet('Preview','Store','Check')][string]$Mode='Preview',
    [int]$ReleaseVersionCode=0,
    [switch]$GoogleMirror,
    [switch]$Offline
)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
if (-not $env:JAVA_HOME) { $env:JAVA_HOME='D:\AndroidToolchain\jdk-17.0.20.1+1' }
if (-not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\javac.exe'))) { throw 'Set JAVA_HOME to JDK 17 first.' }
$env:GRADLE_USER_HOME=Join-Path $projectRoot '.gradle-home'
$env:ANDROID_USER_HOME=Join-Path $projectRoot '.android'
$gradleCommand='D:\AndroidToolchain\gradle\gradle-8.7\bin\gradle.bat'
if (-not (Test-Path -LiteralPath $gradleCommand)) { $gradleCommand=Join-Path $projectRoot 'gradlew.bat' }
$buildArgs=@('--no-daemon','--console=plain')
if($GoogleMirror){$buildArgs+='-PgoogleMirror=true'}
if($Offline){$buildArgs+='--offline'}
if($Mode -eq 'Store'){
    if($ReleaseVersionCode -le 0){throw 'Provide the next published Myket versionCode with -ReleaseVersionCode.'}
    $buildArgs+="-PreleaseVersionCode=$ReleaseVersionCode"
    $buildArgs+=@(':app:storeRelease',':app:testDebugUnitTest',':app:lintRelease')
} elseif($Mode -eq 'Check') {
    $buildArgs+=@(':app:testDebugUnitTest',':app:lintDebug')
} else {
    $buildArgs+=@(':app:assemblePreview',':app:testDebugUnitTest',':app:lintDebug')
}
& $gradleCommand @buildArgs
if($LASTEXITCODE -ne 0){throw "Build failed with exit code $LASTEXITCODE"}
if($Mode -ne 'Check'){
    $variant=if($Mode -eq 'Store'){'release'}else{'preview'}
    $source=Join-Path $projectRoot "app\build\outputs\apk\$variant\app-$variant.apk"
    $artifactCode=if($Mode -eq 'Store'){$ReleaseVersionCode}else{205}
    $destination=Join-Path $projectRoot "releases\JET-2.0.0-build$artifactCode-$variant.apk"
    New-Item -ItemType Directory -Force -Path (Split-Path $destination) | Out-Null
    Copy-Item -LiteralPath $source -Destination $destination -Force
    Get-Item -LiteralPath $destination | Select-Object FullName,Length
    Get-FileHash -LiteralPath $destination -Algorithm SHA256
}

