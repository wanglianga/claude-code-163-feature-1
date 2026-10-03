# syntax=docker/dockerfile:1
# 后端镜像：Maven 构建 + JRE 运行
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
# 上传的现场照片持久化目录（compose 中挂载卷）
RUN mkdir -p /app/uploads
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/manhole \
    SPRING_DATASOURCE_USERNAME=postgres \
    SPRING_DATASOURCE_PASSWORD=manhole
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
