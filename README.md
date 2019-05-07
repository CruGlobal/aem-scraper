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

`java -jar aem-scraper-1.0-SNAPSHOT-jar-with-dependencies.jar <url to top level json>`
