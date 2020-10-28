#FROM openjdk:8-jdk-alpine
FROM koosiedemoer/netty-tcnative-alpine

COPY target/Look-see-0.1.9.jar look-see.jar
COPY GCP-MyFirstProject-1c31159db52c.json GCP-MyFirstProject-1c31159db52c.json
EXPOSE 443
EXPOSE 80
EXPOSE 8080
EXPOSE 9080
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-ea","-jar", "look-see.jar"]