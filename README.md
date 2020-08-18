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
  * Select the `aem-scraper-2.0-SNAPSHOT-jar-with-dependencies.jar`
  * Under Program Arguments, put the full URL to the top level JSON file of the tree you wish to traverse as well as other arguments that don't start with `-D`. If you are using the cloudsearch functionality of this program, you will also need to add basic authentication here.
  * Under VM Options, add the `-D` arguments described below (e.g. `-DrunMode=cloudsearch`)
  
### Command Line
`mvn clean install`

`cd target/`

There are two run modes available: `S3` and `cloudsearch`. These run modes determine what this application is doing.

#### S3
Specifically, the use case for this run mode is to put the data into Amazon S3 and then use the data with
Amazon Comprehend to train a model using custom classification.

`java -jar aem-scraper-1.0-SNAPSHOT-jar-with-dependencies.jar <url to top level json> <s3 bucket name> 
<path to s3 file (not including the file name)> <file or bytes>`

Above, specify `file` if you want the program to generate a csv file on your local machine, otherwise specify `bytes`.

If you have previously generated a csv file and placed it into `./data.csv`, then you can run the program with the 
flag `-DonlySendToS3=true`.

If you want to test what will be inserted into the CSV file without sending it to S3, pass in the flag
`-DonlyBuildCSV=true` when running the program.

If you want to set a log file instead of logging to the console, you can pass in the flag
`-Dorg.slf4j.simpleLogger.logFile={path/to/file}`.

##### Example:
`java -jar aem-scraper-1.0-SNAPSHOT-jar-with-dependencies.jar http://localhost:4503/api/content/site/us/en.json
 my-bucket /first/second/folder file -DonlySendToS3=false -DonlyBuildCSV=false
 -Dorg.slf4j.simpleLogger.logFile=./output.log -DrunMode=S3`

#### cloudsearch
Specifically, the use case for this run mode is to put the data into a set of `.json` files formatted to be uploaded
into Amazon CloudSearch. The upload happens using the CloudSearch UI in AWS, not programmatically here in this app.
These json files will be batches of 5 MB or less.

The second argument should be the type to tell CloudSearch whether these are add or delete requests. 
Valid values are `ADD` and `DELETE`.

You also need to add basic authentication for the server you are trying to scrape.

##### Example 
`java -jar -DrunMode=cloudsearch aem-scraper-1.0-SNAPSHOT-jar-with-dependencies.jar <url to top level json> ADD -u username:password`
