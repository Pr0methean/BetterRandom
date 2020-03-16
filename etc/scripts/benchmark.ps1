$JAVA_HOME = [Environment]::GetEnvironmentVariable($1) # first arg names the variable that JAVA_HOME is copied from
$JAVA_BIN = $JAVA_HOME + '/bin/java'
$RANDOM_DOT_ORG_KEY = $env:RANDOM_DOT_ORG_KEY
$MAYBE_PROGUARD="pre-integration-test"
cd betterrandom
mvn -B "-DskipTests" "-Darguments=-DskipTests" "-Dmaven.test.skip=true" "$MAYBE_ANDROID_FLAG" `
    "clean" "$MAYBE_PROGUARD" install
cd ../benchmark
mvn -B "-DskipTests" "$MAYBE_ANDROID_FLAG" package
cd target
"$JAVA_BIN" -jar benchmarks.jar

