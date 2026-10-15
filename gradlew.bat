@echo off
setlocal
set "GRADLE_VERSION=8.9"
where gradle >nul 2>nul
if %ERRORLEVEL%==0 (
  gradle --version | findstr /C:"Gradle %GRADLE_VERSION%" >nul
  if not errorlevel 1 (
    gradle %*
    exit /b %ERRORLEVEL%
  )
)
echo This project bootstrap wrapper requires Gradle %GRADLE_VERSION% to be installed on Windows.
echo On GitHub Actions, the workflow installs Gradle %GRADLE_VERSION% automatically.
exit /b 1
