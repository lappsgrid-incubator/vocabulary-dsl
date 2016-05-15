#!/bin/bash
mvn package
VERSION=$(cat VERSION)
java -jar target/vocab-$VERSION.jar src/test/resources/lapps.vocab
