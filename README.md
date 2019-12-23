# AEM Scraper
The idea of this project is to be able to crawl the out of the box AEM API 
(e.g. localhost:4503/api/content/sites/mysite.json) and pull out the properties we care about, along with the content.

Specifically, the use case for this project is to put the data into Amazon S3 and then use the data with
Amazon Comprehend to train a model using custom classification.
## To Run

### IntelliJ:
Maven Projects > Execute Maven Goal > clean install

Run > Edit Configurations

Add a JAR Application
  * Select the `aem-scraper-1.0-SNAPSHOT-jar-with-dependencies.jar`
  * Under Program Arguments, put the full URL to the top level JSON file of the tree you wish to traverse
  
### Command Line
`mvn clean install`

`cd target/`

`java -jar aem-scraper-1.0-SNAPSHOT-jar-with-dependencies.jar <url to top level json> <s3 bucket name> 
<path to s3 file (not including the file name)> <file or bytes>`

Above, specify `file` if you want the program to generate a csv file on your local machine, otherwise specify `bytes`.

If you have previously generated a csv file and placed it into `./data.csv`, then you can run the program with the 
flag `-DonlySendToS3=true`.

If you want to test what will be inserted into the CSV file without sending it to S3, pass in the flag
`-DonlyBuildCSV=true` when running the program.

If you want to set a log file instead of logging to the console, you can pass in the flag
`-Dorg.slf4j.simpleLogger.logFile={path/to/file}`.

In order to determine the public facing URLs, we need to use a login to the AEM instance we are hitting.
You can pass the username and password in as parameters when running the JAR with the flags
`-Dusername` and `-Dpassword`.

#### Example:
`java -jar aem-scraper-1.0-SNAPSHOT-jar-with-dependencies.jar http://localhost:4503/api/content/site/us/en.json
 my-bucket /first/second/folder file -DonlySendToS3=false -DonlyBuildCSV=false
 -Dorg.slf4j.simpleLogger.logFile=./output.log -Dusername=myuser -Dpassword=secretz`
