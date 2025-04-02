#FROM sunbird/openjdk-java11-alpine:latest
FROM openjdk:17-slim
MAINTAINER "S M Y ALTAMASH <smy.altamash@gmail.com>"
RUN apt update \
    && apt install -y unzip curl \
    && adduser --uid 1001 --home /home/sunbird/ --disabled-login --gecos "" sunbird \
    && mkdir -p /home/sunbird/
RUN apt update \
    && apt install -y fonts-noto-core fonts-noto-cjk fonts-noto-color-emoji fonts-noto-extra \
    && fc-cache -f

ADD ./certificate-service-1.2.0-dist.zip /home/sunbird/
RUN unzip /home/sunbird/certificate-service-1.2.0-dist.zip -d /home/sunbird/
RUN chown -R sunbird:sunbird /home/sunbird
USER sunbird
EXPOSE 9000
WORKDIR /home/sunbird/
CMD java -XX:+PrintFlagsFinal $JAVA_OPTIONS -cp '/home/sunbird/certificate-service-1.2.0/lib/*' play.core.server.ProdServerStart  /home/sunbird/certificate-service-1.2.0
