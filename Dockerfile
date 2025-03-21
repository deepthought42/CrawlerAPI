FROM adoptopenjdk/openjdk14

#COPY target/crawlerApi-0.1.20.jar crawlerApi.jar
#COPY GCP-MyFirstProject-1c31159db52c.json GCP-MyFirstProject-1c31159db52c.json
COPY gmail_credentials.json /etc/creds/gmail_credentials.json
EXPOSE 443
EXPOSE 80
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-Xms800M", "-jar", "crawlerApi.jar"]