@echo off
setlocal
set VERSION=8.13
set DIST=gradle-%VERSION%-bin.zip
set BASE=https://services.gradle.org/distributions
set CACHE=%USERPROFILE%\.gradle\nihongo-bootstrap
set INSTALL=%CACHE%\gradle-%VERSION%
if exist "%INSTALL%\bin\gradle.bat" goto runGradle
if not exist "%CACHE%" mkdir "%CACHE%"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $base='%BASE%'; $dist='%DIST%'; $cache='%CACHE%'; $zip=Join-Path $cache $dist; $sumFile=Join-Path $cache ($dist+'.sha256'); Invoke-WebRequest ($base+'/'+$dist) -OutFile $zip; Invoke-WebRequest ($base+'/'+$dist+'.sha256') -OutFile $sumFile; $expected=(Get-Content $sumFile -Raw).Trim(); $actual=(Get-FileHash $zip -Algorithm SHA256).Hash.ToLower(); if ($expected.ToLower() -ne $actual) { Remove-Item $zip -Force; throw 'Gradle SHA-256 mismatch' }; Expand-Archive -Path $zip -DestinationPath $cache -Force"
if errorlevel 1 exit /b 1
:runGradle
call "%INSTALL%\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
