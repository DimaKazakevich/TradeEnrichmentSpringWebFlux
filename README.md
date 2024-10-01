# Documentation
### How to run the service
Please do the following steps:
- Clone repository
- Open terminal inside cloned repository path
- ⚠️ Ensure you have maven CLI compatible with Java 17
- Execute `mvn clean install` command in terminal to build the project
- Execute `mvn spring-boot:run` to run project using Maven or execute `cd target` command in terminal to navigate to output folder and then execute `java -jar trade-enrichment-service-0.0.1-SNAPSHOT.jar` to run the app

### How to use the API
- If you wish to send data from a file
```curl
curl --data @src/test/resources/trade.csv --header 'Content-Type: text/csv' http://localhost:8080/api/v1/enrich
```
- If you with to send data directly without file
```curl
curl --location 'http://localhost:8080/api/v1/enrich' \
--header 'Content-Type: text/csv' \
--data 'date,product_id,currency,price
20160101,1,EUR,10.0
20160101,2,EUR,20.1
20160101,3,EUR,30.34
20160101,11,EUR,35.34'
```
### Any limitations of the code.
- `product.csv` file is loading into memory so there is restriction that this file can not have more than 100K rows

### Any discussion/comment on the design.
- Project designed with Spring WebFlux usage to handle large data using asynchronous non-blocking environment
- `Enrich` endpoint returns response as a stream to be able to handle large amount of trades 

### Any ideas for improvement if there were more time available
- Implement separate endpoint to generate the continuous stream of trade events then consume this stream in `enrich` endpoint. This way app will be able to handle large amount of trades and not store full trade.csv file content as http request in memory but handle stream data (http chunks)