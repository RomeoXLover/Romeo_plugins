@echo off
setlocal

set "SCRIPT_DIR=%~dp0"

rem --- Required Gradle version (matches gradle-wrapper.properties) ---
set "GRADLE_VERSION=8.10.2"
if exist "%SCRIPT_DIR%gradle\wrapper\gradle-wrapper.properties" (
  for /f "tokens=2 delims=-" %%A in ('findstr /c:"distributionUrl" "%SCRIPT_DIR%gradle\wrapper\gradle-wrapper.properties"') do (
    set "GRADLE_VERSION=%%A"
  )
)

rem --- Locate a usable JDK ---
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" goto jdk_ok
set "JAVA_HOME="
for /d %%D in ("%ProgramFiles%\Java\*" "%ProgramFiles%\Eclipse Adoptium\*" "%ProgramFiles(x86)%\Java\*") do (
  if not defined JAVA_HOME if exist "%%~D\bin\java.exe" set "JAVA_HOME=%%~D"
)
:jdk_ok
if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"

rem --- Gradle install dir (per user, version-specific) ---
set "GRADLE_DIR=%TEMP%\gradle-%GRADLE_VERSION%"
if not exist "%GRADLE_DIR%\bin\gradle.bat" (
  echo Gradle %GRADLE_VERSION% not found at %GRADLE_DIR%. Downloading...
  set "ZIP=%TEMP%\gradle-%GRADLE_VERSION%-bin.zip"
  powershell -NoProfile -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%ZIP%'"
  powershell -NoProfile -Command "Expand-Archive -Force '%ZIP%' '%TEMP%'"
  del "%ZIP%" 2>nul
)

call "%GRADLE_DIR%\bin\gradle.bat" %*
