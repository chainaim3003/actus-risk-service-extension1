cd C:\actus (my path)

cd C:\actus\actus-riskservice

docker build -t actus-risksrv3-custom:latest .

cd C:\actus\actus-docker-networks

docker compose -f quickstart-docker-actus-rf20.yml down

docker compose -f quickstart-docker-actus-rf20.yml up -d

docker compose -f quickstart-docker-actus-rf20.yml logs actus-riskserver-ce