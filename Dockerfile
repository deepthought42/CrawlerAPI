FROM adoptopenjdk/openjdk14

COPY target/Look-see-0.1.20.jar look-see.jar
COPY GCP-MyFirstProject-1c31159db52c.json GCP-MyFirstProject-1c31159db52c.json
COPY gmail_credentials.json /etc/creds/gmail_credentials.json
EXPOSE 9080
EXPOSE 80
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-Xms800M", "-ea","-jar", "look-see.jar"]
