#FROM sunbird/openjdk-java11-alpine:latest
FROM openjdk:17.0.1-jdk-slim
MAINTAINER "S M Y ALTAMASH <smy.altamash@gmail.com>"
RUN apt update \
    && apt install -y unzip curl fontconfig \
    && apt install -y --no-install-recommends fonts-dejavu fonts-liberation \
    && adduser --uid 1001 --home /home/sunbird/ --disabled-login --gecos "" sunbird \
    && mkdir -p /home/sunbird/
RUN apt update \
    && apt install -y fonts-noto-core fonts-noto-cjk fonts-noto-color-emoji fonts-noto-extra
RUN apt update \
    && apt install -y fonts-dejavu-core fonts-dejavu-extra fonts-liberation
ADD ./certificate-service-1.2.0-dist.zip /home/sunbird/
RUN unzip /home/sunbird/certificate-service-1.2.0-dist.zip -d /home/sunbird/

# create native dir and extract Netty native .so into it so it is not extracted+deleted at runtime
RUN mkdir -p /home/sunbird/native \
    && for jar in /home/sunbird/certificate-service-1.2.0/lib/io.netty.netty-transport-native-epoll*.jar; do \
         [ -f "$jar" ] && unzip -j "$jar" '*/libnetty_transport_native_epoll_*.so' -d /home/sunbird/native || true; \
       done \
    || true

RUN chown -R sunbird:sunbird /home/sunbird

USER sunbird
EXPOSE 9000
WORKDIR /home/sunbird/

ENV JAVA_OPTIONS="\
  -Djava.io.tmpdir=/tmp \
  -Dio.netty.native.workdir=/home/sunbird/native \
  -Xms512m \
  -Xmx600m \
  -XX:+UseG1GC \
  -XX:MaxMetaspaceSize=256m \
  -XX:MaxDirectMemorySize=256m \
  -XX:+UseStringDeduplication \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/tmp/heap.hprof \
  -Xlog:gc*:file=/tmp/gc.log:time,uptime,level,tags:filecount=3,filesize=10M \
"

CMD java $JAVA_OPTIONS -cp '/home/sunbird/certificate-service-1.2.0/lib/*' \
  play.core.server.ProdServerStart /home/sunbird/certificate-service-1.2.0
