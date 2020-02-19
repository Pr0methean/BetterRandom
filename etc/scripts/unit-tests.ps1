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
