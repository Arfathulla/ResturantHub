@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-26.0.1
set PATH=%JAVA_HOME%\bin;%PATH%
echo JAVA_HOME=%JAVA_HOME%
java -version
call mvnw.cmd -B compile -Dmaven.test.skip=true 2>&1
