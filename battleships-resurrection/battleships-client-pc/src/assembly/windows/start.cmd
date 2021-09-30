set "BATTLESHIPS_HOME=%cd%"
set JAVA_EXTRA_GFX_FLAGS="-Dsun.java2d.opengl=true"
set JAVA_RUN="jre\bin\java.exe"
%JAVA_RUN% %JAVA_FLAGS% %JAVA_EXTRA_GFX_FLAGS% "-Djava.library.path=%BATTLESHIPS_HOME%" -jar "%BATTLESHIPS_HOME%/battleships-resurrection.jar" %*
