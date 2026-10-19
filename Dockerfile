
FROM eclipse-temurin:17-jdk-jammy

LABEL maintainer="S M Y ALTAMASH <smy.altamash@gmail.com>"

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        unzip \
        curl \
        ca-certificates \
        fontconfig \
        fonts-dejavu \
        fonts-dejavu-core \
        fonts-dejavu-extra \
        fonts-liberation \
        fonts-noto-core \
        fonts-noto-cjk \
        fonts-noto-color-emoji \
        fonts-noto-extra \
        libxrender1 \
        libjpeg-turbo8 \
        libxtst6 \
        xfonts-75dpi \
        xfonts-base \
        xz-utils \
    && rm -rf /var/lib/apt/lists/* \
    && adduser --uid 1001 --home /home/sunbird/ --disabled-login --gecos "" sunbird \
    && mkdir -p /home/sunbird/

COPY ./certificate-service-1.2.0-dist.zip /home/sunbird/

RUN unzip /home/sunbird/certificate-service-1.2.0-dist.zip -d /home/sunbird/ \
    && chown -R sunbird:sunbird /home/sunbird/ \
    && rm /home/sunbird/certificate-service-1.2.0-dist.zip

USER sunbird

WORKDIR /home/sunbird/

EXPOSE 9000

CMD ["sh", "-c", "exec java -XX:+PrintFlagsFinal $JAVA_OPTIONS -cp '/home/sunbird/certificate-service-1.2.0/lib/*' play.core.server.ProdServerStart /home/sunbird/certificate-service-1.2.0"]