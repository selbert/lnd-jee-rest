#!/bin/sh
mvn clean package && docker build -t ch.puzzle.ek/lnd-api .
docker rm -f lnd-api || true && docker run -d -p 8080:8080 -p 4848:4848 --name lnd-api ch.puzzle.ek/lnd-api 
