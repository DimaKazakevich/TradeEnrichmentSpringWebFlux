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
curl --data-binary @src/test/resources/trade.csv -H 'Transfer-Encoding: chunked' -H 'Content-Type: text/plain' http://localhost:8080/api/v1/enrich
```
- If you with to send data directly without file
```curl
curl --location 'http://localhost:8080/api/v1/enrich' \
--header 'Content-Type: text/plain' \
--header 'Transfer-Encoding: chunked' \
--data 'date,product_id,currency,price
20160101,1,EUR,10.0
20160101,2,EUR,20.1
20160101,3,EUR,30.34
20160101,11,EUR,35.34'
```
### Any limitations of the code.
- `product.csv` file is loading into memory because there is restriction that this file can not have more than 100K 
  rows. The maximum capacity of this file is adjustable via `PRODUCT_MAPPING_FILE_MAX_CAPACITY` env variable but it has 
  default value of 100K rows as you can find in `application.yaml` file

### Any discussion/comment on the design.
- Project designed with Spring WebFlux usage to handle large data using asynchronous non-blocking environment
- `enrich` endpoint accept request as stream so all request data will not be stored in the memory but handled as a 
  chunks what allows large scaling
- To accept request as a stream `Tomcat` server is excluded and `Reactor Netty` is added as a server in `pom.xml`
- `enrich` endpoint returns response as a stream to be able to handle large amount of trades
- `Content-type` request header value has been changed to `text/plain` to accept `Flux<DataBuffer>` as Request Body 
  because it's generally better when we expect millions of rows. It allows the 
  application to start processing the data as soon as it starts receiving it, without waiting for the entire String 
  to be read into memory. Using this approach we can implement backpressure effectively and process data chunks 
  on-the-fly, which can be more efficient in a non blocking, reactive environment.
- `Content-type` response header value has been changed to `text/event-stream`. This is used for Server-Sent Events 
  (SSE) in reactive streams, allowing the server to push events to the client as they occur. So we want to stream 
  parts of the data back as they are processed.

### Any ideas for improvement if there were more time available
- Ideally requirements have to be updated to have request in JSON format. This would greatly reduce development time 
  because it allows us to accept `Flux<TradeInput>` as `@RequestBody` and handle data way more easily. In my 
  solution, I accept `Flux<DataBuffer>` as `@RequestBody` and then do a lot of conversions to convert it into 
  `Flux<TradeInput>`. All these conversions are using blocking code under the hood that is why I added `Schedulers.
  boundedElastic()` in most places to execute blocking code as part of reactive flow. While development I 
  added even `BlockHound` library to detect blocking code. This could have been avoided if the data had been in a JSON 
  format. Perhaps another solution would be to write something like own `HttpMessageConverter` for WebFlux's Jackson to 
  accept `csv` data as `Flux<TradeInput>` in a request body, but again that's if there was more time
- Error handling has to be improved
- Improve `enrich` controller method return type from current `ResponseEntity<Flux<String>>` to 
  `Flux<ResponseEntity<Flux<String>>>` to provide response status, headers, asynchronously at a later 
  point. It allows the response status and headers to vary depending on the outcome of asynchronous request handling.