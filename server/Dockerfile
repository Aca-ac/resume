# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY .mvn/settings.xml .mvn/settings.xml
RUN mvn -s .mvn/settings.xml dependency:go-offline -B -q
COPY src ./src
# 去除 UTF-8 BOM 头
RUN find src -name "*.java" -exec sed -i '1s/^\xEF\xBB\xBF//' {} \;
RUN mvn -s .mvn/settings.xml package -DskipTests -B -q

# Stage 2: Run
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r app && useradd -r -g app app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]