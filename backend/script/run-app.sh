#!/bin/bash
# Run the Auction App with both Spring Boot API and JavaFX UI

cd "$(dirname "$0")"

echo "Starting Auction App with Spring Boot API and JavaFX UI..."
echo "Using: mvn javafx:run"
echo ""

bash ./mvnw javafx:run
