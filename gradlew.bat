@echo off
setlocal
set "JAVA_HOME=%JAVA_HOME:-/usr/local/sdkman/candidates/java/21.0.12+1-ms%"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "GRADLE_HOME=%GRADLE_HOME:-/tmp/gradle-8.10.2%"
if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  echo Gradle 8.10.2 is not available at %GRADLE_HOME%. Download it first.
  exit /b 1
)
call "%GRADLE_HOME%\bin\gradle.bat" %*
