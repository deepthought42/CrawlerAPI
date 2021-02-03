[![Codacy Badge](https://app.codacy.com/project/badge/Grade/e2376d355755402aaa5bf7c533750851)](https://www.codacy.com?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=deepthought42/WebTestVisualizer&amp;utm_campaign=Badge_Grade)

# Getting Started

## Launch Jar locally


### Command Line Interface(CLI)

maven clean install
java -ea -jar target/Look-see-#.#.#.jar

NOTE: The `-ea` flag tells the java compiler to run the program with assertions enabled


### Docker

maven clean install
docker build --tag look-see .
docker run -p 80:80 -p 8080:8080 -p 9080:9080 -p 443:443 --name look-see look-see


### Deploy docker container to gcr
docker build --no-cache -t gcr.io/cosmic-envoy-280619/look-see-api:v#.#.# .

docker push gcr.io/cosmic-envoy-280619/look-see-api:v#.#.#