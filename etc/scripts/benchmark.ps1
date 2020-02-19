$JAVA_HOME = [Environment]::GetEnvironmentVariable($1) # first arg names the variable that JAVA_HOME is copied from
echo "JAVA_HOME: $JAVA_HOME"
$JAVA_BIN = $JAVA_HOME + '/bin/java'
if ( $env:ANDROID )
{
    $MAYBE_ANDROID_FLAG = "-Pandroid"
}
else
{
    $MAYBE_ANDROID_FLAG = ""
}
if ( $env:APPVEYOR )
{
    $RANDOM_DOT_ORG_KEY = $env:random_dot_org_key
}
$MAYBE_PROGUARD="pre-integration-test"
cd betterrandom
mvn -B "-DskipTests" "-Darguments=-DskipTests" "-Dmaven.test.skip=true" "$MAYBE_ANDROID_FLAG" `
    "clean" "$MAYBE_PROGUARD" install
cd ../benchmark
mvn -B "-DskipTests" "$MAYBE_ANDROID_FLAG" package
cd target
if ( $TRAVIS ) {
  java -jar benchmarks.jar -f 1 -t 1 -foe true
  java -jar benchmarks.jar -f 1 -t 2 -foe true
} else {
  java -jar benchmarks.jar -f 1 -t 1 -foe true -v EXTRA 2>&1 | `
      Tee-Object benchmark_results_one_thread.txt
  java -jar benchmarks.jar -f 1 -t 2 -foe true -v EXTRA 2>&1 | `
      Tee-Object benchmark_results_two_threads.txt
}
