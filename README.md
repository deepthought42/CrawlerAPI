DOCKER

docker build --tag look-see .
docker run -p 80:80 -p 8080:8080 -p 9080:9080 -p 443:443 --name look-see look-see
