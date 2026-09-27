#!/bin/bash
# 启动后端（Spring Boot，端口 8080）
cd "$(dirname "$0")/../backend" && mvn spring-boot:run
