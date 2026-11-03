@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Maven Wrapper Startup Batch script for Windows
@REM ----------------------------------------------------------------------------

@IF "%DEBUG%"=="" @ECHO OFF
SETLOCAL ENABLEEXTENSIONS ENABLEDELAYEDEXPANSION

SET "WDIR=%~dp0"
IF NOT "%WDIR:~-1%"=="\" SET "WDIR=%WDIR%\"
SET "MAVEN_PROJECTBASEDIR=%WDIR%"

IF NOT EXIST "%MAVEN_PROJECTBASEDIR%pom.xml" (
  ECHO Error: pom.xml not found in %MAVEN_PROJECTBASEDIR%
  EXIT /B 1
)

SET "WRAPPER_PROP=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties"

@REM 1. Validate Java presence
SET "JAVACMD="
IF NOT "%JAVA_HOME%"=="" (
  IF EXIST "%JAVA_HOME%\bin\java.exe" (
    SET "JAVACMD=%JAVA_HOME%\bin\java.exe"
  )
)
IF "%JAVACMD%"=="" (
  WHERE java.exe >nul 2>nul
  IF NOT ERRORLEVEL 1 (
    SET "JAVACMD=java.exe"
  ) ELSE IF EXIST "C:\Program Files\Java\jdk-26.0.1\bin\java.exe" (
    SET "JAVACMD=C:\Program Files\Java\jdk-26.0.1\bin\java.exe"
  ) ELSE (
    ECHO Error: JAVA_HOME is not set and no 'java' executable could be found in your PATH.
    EXIT /B 1
  )
)

@REM 2. Read distributionUrl from wrapper properties
SET "DIST_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip"
IF EXIST "%WRAPPER_PROP%" (
  FOR /F "usebackq tokens=1,* delims==" %%A IN ("%WRAPPER_PROP%") DO (
    SET "KEY=%%A"
    SET "VAL=%%B"
    SET "KEY=!KEY: =!"
    IF "!KEY!"=="distributionUrl" (
      SET "DIST_URL=!VAL!"
    )
  )
)

@REM 3. Determine distribution paths in user profile
FOR %%F IN ("!DIST_URL!") DO SET "ZIP_NAME=%%~nxF"
FOR %%F IN ("!DIST_URL!") DO SET "DIR_NAME=%%~nF"
SET "DIR_NAME=!DIR_NAME:-bin=!"

SET "WRAPPER_DIR=%USERPROFILE%\.m2\wrapper\dists\!DIR_NAME!"
SET "MAVEN_HOME=!WRAPPER_DIR!\!DIR_NAME!"
SET "MVN_CMD=!MAVEN_HOME!\bin\mvn.cmd"

IF NOT EXIST "!MVN_CMD!" (
  @REM Check if already extracted under subfolder
  IF EXIST "!WRAPPER_DIR!" (
    FOR /D %%D IN ("!WRAPPER_DIR!\*") DO (
      IF EXIST "%%D\bin\mvn.cmd" (
        SET "MVN_CMD=%%D\bin\mvn.cmd"
      )
    )
  )
)

@REM 4. Download and extract if missing
IF NOT EXIST "!MVN_CMD!" (
  ECHO [Maven Wrapper] Apache Maven not found locally.
  ECHO [Maven Wrapper] Downloading !DIST_URL!...
  IF NOT EXIST "!WRAPPER_DIR!" (
    MKDIR "!WRAPPER_DIR!"
  )
  SET "ZIP_TARGET=!WRAPPER_DIR!\!ZIP_NAME!"
  powershell -NoProfile -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object System.Net.WebClient).DownloadFile('!DIST_URL!', '!ZIP_TARGET!'); Expand-Archive -Path '!ZIP_TARGET!' -DestinationPath '!WRAPPER_DIR!' -Force"
  IF ERRORLEVEL 1 (
    ECHO [Maven Wrapper] Error: Failed to download or unpack Maven distribution.
    EXIT /B 1
  )
  IF EXIST "!ZIP_TARGET!" (
    DEL /F /Q "!ZIP_TARGET!"
  )
  @REM Re-scan for mvn.cmd in extracted directory
  FOR /D %%D IN ("!WRAPPER_DIR!\*") DO (
    IF EXIST "%%D\bin\mvn.cmd" (
      SET "MVN_CMD=%%D\bin\mvn.cmd"
    )
  )
  IF NOT EXIST "!MVN_CMD!" (
    IF EXIST "!WRAPPER_DIR!\bin\mvn.cmd" (
      SET "MVN_CMD=!WRAPPER_DIR!\bin\mvn.cmd"
    )
  )
)

IF NOT EXIST "!MVN_CMD!" (
  ECHO [Maven Wrapper] Error: Could not locate mvn.cmd after extraction in !WRAPPER_DIR!
  EXIT /B 1
)

@REM 5. Execute Maven with arguments
CALL "!MVN_CMD!" %*
SET "EXIT_CODE=!ERRORLEVEL!"

ENDLOCAL & EXIT /B %EXIT_CODE%
