[![Codacy Badge](https://app.codacy.com/project/badge/Grade/e2376d355755402aaa5bf7c533750851)](https://www.codacy.com?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=deepthought42/WebTestVisualizer&amp;utm_campaign=Badge_Grade)

# Getting Started

## Launch Jar locally


### Command Line Interface(CLI)

	```bash
	maven clean install
	java -ea -jar target/crawlerApi-#.#.#.jar
	```

	NOTE: The `-ea` flag tells the java compiler to run the program with assertions enabled

### Neo4j application setup

Note that this section will need to be replaced once we have an Anthos or Terraform script. 

Step 1: setup firewall for neo4j

	```bash
	gcloud compute firewall-rules create allow-neo4j-bolt-http-https --allow tcp:7473,tcp:7474,tcp:7687 --source-ranges 0.0.0.0/0 --target-tags neo4j
	```

Step 2: Get image name for Community version 1.4

	```bash
	gcloud compute images list --project launcher-public | grep --extended-regexp "neo4j-community-1-4-.*"
	```

Step 3: create new instance

	```bash
	gcloud config set project cosmic-envoy-280619
	gcloud compute instances create neo4j-prod --machine-type e2-medium --image-project launcher-public --image neo4j-community-1-4-3-6-apoc --tags neo4j,http-server,https-server
	gcloud compute instances add-tags neo4j-stage --tags http-server,https-server
	```

Step 4 : SSH to server and check status

	```bash
	gcloud compute ssh neo4j-stage
	sudo systemctl status neo4j
	```

Follow step 3 from this webpage to configure neo4j server - https://www.digitalocean.com/community/tutorials/how-to-install-and-configure-neo4j-on-ubuntu-20-04

Step 6: Delete neo4j instance

	```bash
	gcloud compute instances delete neo4j-stage
	```

### Docker

	```bash
	maven clean install
	docker build --tag crawlerApi .
	docker run -p 80:80 -p 8080:8080 -p 9080:9080 -p 443:443 --name crawlerApi crawlerApi
	```


### Deploy Docker container to GCP Artifact Registry

	```bash
	gcloud auth print-access-token | sudo docker login -u oauth2accesstoken --password-stdin https://us-central1-docker.pkg.dev
	sudo docker build --no-cache -t us-central1-docker.pkg.dev/cosmic-envoy-280619/api/#.#.# .
	sudo docker push us-central1-docker.pkg.dev/cosmic-envoy-280619/api/#.#.#
	```

# Security

## Configuring SSL with GoDaddy - This shoudl be replaced with a more reasonoble provider by February 2024

When a new certificate is issued by GoDaddy it doesn't have the right key and we don't get the private key from them for whatever reason. Therefore it's necessary to re-key the certificate. 

	1. Retrieve the crawlerApi.com CSR from keys folder
	2. Paste CSR into field for re-keying SSL certification on GoDaddy
	3. Select add change and download files

After ensuring that all certificates are generated with the proper CSR you need to combine the CRT files provided by GoDaddy. On the command line, navigate to the folder where the downloaded certificates are located and run the following

	cat c4f2f133293785ad.crt gd_bundle-g2-g1.crt > crawlerApi.com-chain.crt
	

## Generating a new PKCS12 certificate for SSL

Run the following command in Linux to create a keystore called api_key with a privateKeyEntry


* Using CRT

	```bash
	openssl pkcs12 -export -inkey private.key -in certificate.crt -out api_key.p12
	```

* Using PEM instead

	```bash
	openssl pkcs12 -export -inkey crawlerApi.com.key -in crawlerApi.com-2022-chain.pem -out api_key.p12
	```