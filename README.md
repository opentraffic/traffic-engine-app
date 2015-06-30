# Traffic Engine Application
A GPS processing and visualization environment for producing traffic speed data sets.

## Installation Instructions

Requires Java 1.8+

1. Download latest traffic-engine-app.jar (in project releases or build from source using mvn package)
2. Copy application.conf (https://github.com/opentraffic/traffic-engine-app/blob/master/application.conf)  into same directory as jar file.
3. Run applicaiton using:
```
 java -jar traffic-engine-app.jar
```
* View web application at http://localhost:4567/
* Use csv-loader project (https://github.com/opentraffic/csv-loader) to submit CSV data sets for processing

## Build Instructions

1. Git clone
2. mvn package
3. Built package in target/traffic-engine-app.jar 
