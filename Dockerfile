FROM ubuntu:18.04

#install some required basics and jdk
RUN apt-get update
RUN apt-get install -y openjdk-8-jdk

COPY target/Look-see-0.1.0.jar /look-see.jar

ENTRYPOINT ["java","-jar", "/look-see.jar"]