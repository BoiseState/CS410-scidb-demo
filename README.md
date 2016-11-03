# SciDB Demo

This is the SciDB demo application from CS 410 at Boise State University.

## Running

Run with Maven:

    mvn exec:java -Ddb.password=wombat
    
Package and run:

    mvn package
    ./target/scidb-demo/bin/scidb-demo postgresql://user:password@localhost/scidb
    
## Docker Image

This can also be deployed as a Docker image.  Make sure you have a dump file
(available separately) saved as `scidb-dump.sql`, and run:

    mvn package
    docker build -t scidb .
    docker run -d -P --name=scidb scidb
    docker ps
    
That will show you the port that it is listening on. Connect to that port with your browser.

Note that the Docker image sets up PostgreSQL with no security, but does not expose it outside the Docker container.