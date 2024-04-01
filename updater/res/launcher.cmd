@echo off
echo Autorecorder Launcher
java -jar %LOCALAPPDATA%\autorecorder-frc\updater.jar
for /f %%i in ('dir /b /s autorecorder-frc*.jar') do set RESULT=%%i
echo Launching...
for /f "delims= " %%a in ('"wmic path win32_useraccount where name='%UserName%' get sid"') do (
   if not "%%a"=="SID" (
      set SID=%%a
      goto :loop_end
   )
)

:loop_end
icacls %LOCALAPPDATA%\autorecorder-frc /grant *"%SID%":F
start /B "Autorecorder FRC" "javaw" "-jar" "%RESULT%" >> log.txt