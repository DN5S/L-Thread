#!/bin/bash
exec java -XX:+UseZGC -XX:MaxRAMPercentage=75.0 -Dspring.data.redis.host=${SPRING_DATA_REDIS_HOST:-localhost} -Dspring.data.redis.port=${SPRING_DATA_REDIS_PORT:-6379} -jar app.jar