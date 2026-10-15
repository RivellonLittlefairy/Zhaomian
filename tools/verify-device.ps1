param(
    [string]$Serial,
    [switch]$Soak
)
$ErrorActionPreference = 'Stop'
$projectPath = Split-Path -Parent $PSScriptRoot
$adbPath = Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
$studioJdk = 'C:/Program Files/Android/Android Studio/jbr'
if (-not (Test-Path -LiteralPath $adbPath)) { throw '找不到 adb，请先安装 Android SDK platform-tools。' }
if (Test-Path -LiteralPath $studioJdk) { $env:JAVA_HOME = $studioJdk }
if (-not $Serial) {
    $devices = @(& $adbPath devices | Select-String '^\S+\s+device$' | ForEach-Object { ($_.Line -split '\s+')[0] })
    if ($devices.Count -ne 1) { throw '需要且只能连接一台设备，或使用 -Serial 指定设备。' }
    $Serial = $devices[0]
}
Push-Location $projectPath
try {
    & .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
    if ($LASTEXITCODE -ne 0) { throw '构建或单元测试失败。' }
    Write-Host '手机若弹出 USB 安装确认，请点允许；不会自动卸载 App 或清除数据。'
    & $adbPath -s $Serial install --user 0 -r 'app/build/outputs/apk/debug/app-debug.apk'
    if ($LASTEXITCODE -ne 0) { throw 'App 安装被拒绝或失败，请检查手机安装确认。' }
    & $adbPath -s $Serial install --user 0 -r -t 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
    if ($LASTEXITCODE -ne 0) { throw '测试 APK 安装失败。' }
    & $adbPath -s $Serial shell pm grant --user 0 com.example.passerwatch android.permission.CAMERA
    if ($LASTEXITCODE -ne 0) { throw '相机授权失败。' }
    $runnerArguments = @('shell', 'am', 'instrument', '--user', '0', '-w', '-r')
    if ($Soak) {
        Write-Host '进行 30 分钟长测：请保持手机前台，不锁屏、不切换 App。'
        $runnerArguments += @('-e', 'class', 'com.example.passerwatch.FrontScreenSoakTest', '-e', 'soakMinutes', '30')
    }
    $runnerArguments += 'com.example.passerwatch.test/androidx.test.runner.AndroidJUnitRunner'
    $testOutput = & $adbPath -s $Serial @runnerArguments 2>&1
    $runnerExit = $LASTEXITCODE
    $logName = if ($Soak) { 'docs/device-soak.log' } else { 'docs/device-regression.log' }
    $testOutput | Set-Content -LiteralPath $logName -Encoding utf8
    $testOutput | Select-Object -Last 35 | Write-Host
    if ($runnerExit -ne 0 -or ($testOutput -match 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed') -or -not ($testOutput -match 'OK \(')) {
        throw "设备测试未通过，查看 $logName。"
    }
    if ($Soak) {
        & $adbPath -s $Serial logcat -d -s 'ZhaomianSoak:I' '*:S' | Set-Content 'docs/device-soak-memory.log' -Encoding utf8
    }
    Write-Host "测试通过，日志：$logName。"
} finally { Pop-Location }
