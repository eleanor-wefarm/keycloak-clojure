#!/usr/bin/env bash

clojure -A:dev:test -m kaocha.runner "$@"
