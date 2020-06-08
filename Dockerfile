FROM ubuntu:18.04

#install some required basics and jdk
RUN apt-get update
RUN apt-get install -y openjdk-8-jdk

COPY . .

CMD["java","-jar", "target/Qanairy-0.4.0.jar"]