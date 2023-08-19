FROM eclipse-temurin:17-jre-alpine
ARG JAR_FILE=/build/libs/spring-action-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} /ROOT.jar
ENTRYPOINT ["java","-jar","/ROOT.jar"]