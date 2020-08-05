FROM openjdk:8-jdk-alpine

COPY target/Look-see-0.1.0.jar /look-see.jar

EXPOSE 443
EXPOSE 80
EXPOSE 8080
EXPOSE 9687
ENTRYPOINT ["java","-jar", "/look-see.jar"]