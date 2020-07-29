# Getting Started

## Launch Jar locally


### Command Line Interface(CLI)

maven clean install
java -ea -jar target/Look-ee-0.1.0.jar

NOTE: The `-ea` flag tells the java compiler to run the program with assertions enabled


### Docker

maven clean install
docker build --tag look-see .
docker run -p 80:80 -p 8080:8080 -p 9080:9080 -p 443:443 --name look-see look-see
