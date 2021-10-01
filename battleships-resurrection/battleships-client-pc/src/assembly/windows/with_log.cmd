set "BATTLESHIPS_HOME=%cd%"
set "LOG_FILE=%BATTLESHIPS_HOME%/console.log"

rem uncomment the line below if graphics works slowly
set JAVA_EXTRA_GFX_FLAGS="-Dsun.java2d.opengl=true"
rem set JAVA_EXTRA_GFX_FLAGS="-Dcom.sun.management.jmxremote=true -Dsun.java2d.opengl=true"

set JAVA_RUN="jre\bin\java.exe"

echo %%JAVA_RUN%%=%JAVA_RUN% > "%LOG_FILE%"

echo ------JAVA_VERSION------ >> "%LOG_FILE%"

%JAVA_RUN% -version 2>> "%LOG_FILE%"

echo ------------------------ >> ""%LOG_FILE%"

%JAVA_RUN% %JAVA_FLAGS% %JAVA_EXTRA_GFX_FLAGS% "-Djava.library.path=%BATTLESHIPS_HOME%" -jar "%BATTLESHIPS_HOME%/battleships-resurrection.jar" %* 2>> "%LOG_FILE%"
