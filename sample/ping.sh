#!/usr/bin/env bash

curl -X POST -H "Content-Type: Application/json" -H "Accept: Application/text" --data-binary "@request.json" http://localhost:8080/
