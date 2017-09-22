cd betterrandom
mvn clean jacoco:prepare-agent test jacoco:report -e
if %errorlevel% neq 0 exit /b %errorlevel%
rem TODO: Add Coveralls reporting