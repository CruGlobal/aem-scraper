java -jar aem-scraper-1.0-SNAPSHOT-jar-with-dependencies.jar \
http://localhost:4503/api/content/site/us/en.json \
my-bucket \
/first/second/folder \
file \
-DonlySendToS3=false \
-DonlyBuildCSV=false \
-Dorg.slf4j.simpleLogger.logFile=./output.log
