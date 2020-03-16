$JAVA_HOME = [Environment]::GetEnvironmentVariable($env:JAVA_HOME_SOURCE) # names the variable that JAVA_HOME is copied from
if (!$JAVA_HOME) {
  echo "JAVA_HOME not set from" $JAVA_HOME_SOURCE
  exit 1
}
$RANDOM_DOT_ORG_KEY = $env:RANDOM_DOT_ORG_KEY
cd betterrandom
mvn "$MAYBE_ANDROID_FLAG" "clean" "compile" "jacoco:instrument" "jacoco:prepare-agent" `
    "test" "jacoco:restore-instrumented-classes" "jacoco:report" -e
$STATUS = $?
if ( $STATUS ) {
    if ( $JAVA8 ) {
        echo "[unit-tests.ps1] Running Proguard."
        mvn -DskipTests "-Dmaven.test.skip=true" "$MAYBE_ANDROID_FLAG" package pre-integration-test
        echo "[unit-tests.ps1] Testing against Proguarded jar."
        # FIXME: This runs Proguard again after it finishes.
        mvn '-Dmaven.main.skip=true' "$MAYBE_ANDROID_FLAG" "integration-test" "-e" "-B"
        $STATUS = $?
    }
}
cd ..
if ( ! $STATUS ) {
    exit 1
}
