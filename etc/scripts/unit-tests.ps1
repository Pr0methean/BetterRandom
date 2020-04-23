$JAVA_HOME = [Environment]::GetEnvironmentVariable($env:JAVA_HOME_SOURCE) # names the variable that JAVA_HOME is copied from
if (!$JAVA_HOME) {
  echo "JAVA_HOME not set from" $JAVA_HOME_SOURCE
  exit 1
}
$RANDOM_DOT_ORG_KEY = $env:RANDOM_DOT_ORG_KEY
cd betterrandom
mvn "clean" "compile" "jacoco:instrument" "jacoco:prepare-agent" `
    "test" "jacoco:restore-instrumented-classes" "jacoco:report" -e
$STATUS = $?
if ( $STATUS ) {
    if ( $env:PROGUARD -eq 'true' ) {
        echo "[unit-tests.ps1] Running Proguard."
        mvn -DskipTests "-Dmaven.test.skip=true" package proguard:proguard
        echo "[unit-tests.ps1] Testing against Proguarded jar."
        mvn '-Dmaven.main.skip=true' "integration-test" "-e" "-B"
        $STATUS = $?
    } else {
        echo "[unit-tests.ps1] Proguard not enabled."
    }
}
cd ..
if ( ! $STATUS ) {
    exit 1
}
