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
$MAYBE_JACOCO_PREPARE = "compile jacoco:instrument jacoco:prepare-agent"
$MAYBE_JACOCO_REPORT = "jacoco:restore-instrumented-classes jacoco:report"
cd betterrandom
mvn "$MAYBE_ANDROID_FLAG" "help:active-profiles" "clean" "$MAYBE_JACOCO_PREPARE" `
    "test" "$MAYBE_JACOCO_REPORT" -e
$STATUS = $?
if ( $STATUS ) {
    if ( $env:TRAVIS ) {
        $COMMIT = "$env:TRAVIS_COMMIT"
        $JOB_ID = "travis_$env:TRAVIS_JOB_NUMBER"
    } elseif ( $env:APPVEYOR ) {
        $GH_TOKEN = "$env:access_token"
        $COMMIT = "$env:APPVEYOR_REPO_COMMIT"
        $JOB_ID = "appveyor_$env:APPVEYOR_BUILD_NUMBER.$env:APPVEYOR_JOB_NUMBER"
        git config --global user.email "appveyor@appveyor.com"
    } else {
    # Not in CI
        $COMMIT = git rev-parse HEAD
        $JOB_ID = [guid]::NewGuid()
    }
    git clone "https://github.com/Pr0methean/betterrandom-coverage.git"
    cd betterrandom-coverage
    if ( Test-Path $COMMIT ) {
        echo "[unit-tests.bat] Aggregating with JaCoCo reports from other jobs."
        cp "$COMMIT/*.exec" target
        cp ../pom.xml .
        mvn "jacoco:report-aggregate"
        rm pom.xml
        $JACOCO_DIR = "jacoco-aggregate"
    } else {
        echo "[unit-tests.bat] This is the first JaCoCo report for this build."
        mkdir "$COMMIT"
        $JACOCO_DIR = "jacoco"
    }
    mv "../target/jacoco.exec" "$COMMIT/$JOB_ID.exec"
    cd "$COMMIT"
    git add .
    git commit -m "Coverage report from job $JOB_ID"
    git remote add originauth "https://$GH_TOKEN@github.com/Pr0methean/betterrandom-coverage.git"
    git push --set-upstream originauth master
    while (! $?) {
        git pull --rebase  # Merge
        cp "*.exec" "../../target/"
        mvn "jacoco:report-aggregate"
        git push
    }
    cd "../.."
    if ( $TRAVIS ) {
        # Coveralls doesn't seem to work in non-.NET Appveyor yet
        # so we have to hope Appveyor pushes its Jacoco reports before Travis does! :(
        mvn "coveralls:report"
        # Send coverage to Codacy
        Invoke-WebRequest -Uri 'https://github.com/codacy/codacy-coverage-reporter/releases/download/2.0.0/codacy-coverage-reporter-2.0.0-assembly.jar' -OutFile codacy.jar
        java -jar codacy.jar -l Java -r "target/site/$JACOCO_DIR/jacoco.xml"
        # Send coverage to Codecov (copied from the README at https://github.com/codecov/codecov-exe)
        (New-Object System.Net.WebClient).DownloadFile("https://github.com/codecov/codecov-exe/releases/download/1.0.3/Codecov.zip", (Join-Path $pwd "Codecov.zip")) # Download Codecov.zip from github release.
        Expand-Archive .\Codecov.zip -DestinationPath . # UnZip the file.
        .\Codecov\codecov.exe # Run codecov.exe with whatever commands you need.
        git config --global user.email "travis@travis-ci.org"
    }
    if ( ! $JAVA9 ) {
        echo "[unit-tests.bat] Running Proguard."
        mvn -DskipTests "-Dmaven.test.skip=true" "$MAYBE_ANDROID_FLAG" package pre-integration-test
        echo "[unit-tests.bat] Testing against Proguarded jar."
        # FIXME: This runs Proguard again after it finishes.
        mvn "$MAYBE_ANDROID_FLAG" "integration-test" "-e"
        $STATUS = $?
    }
}
if ( ! $STATUS ) {
    echo ""
    echo "[unit-tests.bat] SUREFIRE LOGS"
    echo "[unit-tests.bat] ============="
    foreach ($file in $(Get-ChildItem "target/surefire-reports")) {
        echo ""
        echo "File $file contents:"
        echo $(Get-Content -Path "target/surefire-reports/$file")
    }
    exit 1
} else {
    cd ..
}
