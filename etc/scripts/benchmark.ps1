$JAVA_HOME = [Environment]::GetEnvironmentVariable($env:JAVA_HOME_SOURCE) # names the variable that JAVA_HOME is copied from
if (!$JAVA_HOME) {
  echo "JAVA_HOME not set from" $JAVA_HOME_SOURCE
  exit 1
}
$JAVA_BIN = $JAVA_HOME + '/bin/java'
$RANDOM_DOT_ORG_KEY = $env:RANDOM_DOT_ORG_KEY
cd betterrandom
mvn -B "-DskipTests" "-Darguments=-DskipTests" "-Dmaven.test.skip=true" "clean" "package"
if ( $env:PROGUARD ) {
  echo "[benchmark.ps1] Running Proguard."
  mvn -B -DskipTests "-DskipTests" "-Darguments=-DskipTests" "-Dmaven.test.skip=true" proguard:proguard
} else {
  echo "[benchmark.ps1] Proguard not enabled."
}
mvn -B -DskipTests "-DskipTests" "-Darguments=-DskipTests" "-Dmaven.test.skip=true" "-Dmaven.main.skip=true" install
cd ../benchmark
mvn -B "-DskipTests" package
cd target
$ARGS = '-Djdk.logger.finder.error=QUIET', '-jar', 'benchmarks.jar', '-jvm', ${JAVA_BIN}

# FIXME: PowerShell exits with an error at the following step:
# java.exe : WARNING: An illegal reflective access operation has occurred
# At D:\a\1\s\etc\scripts\benchmark.ps1:14 char:1
# + & $JAVA_BIN $ARGS *>&1
# + ~~~~~~~~~~~~~~~~~~~~~~
#     + CategoryInfo          : NotSpecified: (WARNING: An ill...on has occurred:String) [], RemoteException
#     + FullyQualifiedErrorId : NativeCommandError
& $JAVA_BIN $ARGS *>&1

