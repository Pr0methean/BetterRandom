cd betterrandom
mvn -DskipTests $MAYBE_ANDROID_FLAG clean package install
if %errorlevel% neq 0 exit /b %errorlevel%
cd ../benchmark
mvn -DskipTests $MAYBE_ANDROID_FLAG clean package
if %errorlevel% neq 0 exit /b %errorlevel%
cd target
java -jar benchmarks.jar -f 1 -t 1 -foe true
if %errorlevel% neq 0 exit /b %errorlevel%
java -jar benchmarks.jar -f 1 -t 2 -foe true