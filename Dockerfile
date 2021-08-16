FROM node:12.14 as builder-front

WORKDIR /usr/src/app
COPY ./ergo-faucet-ui/package.json ./
RUN npm install
COPY ./ergo-faucet-ui ./
RUN npm run build

FROM openjdk:8-jre-slim as builder_code
ENV DEBIAN_FRONTEND noninteractive
RUN apt-get update && \
    apt-get install -y --no-install-recommends apt-transport-https apt-utils bc dirmngr gnupg && \
    echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list && \
    echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee /etc/apt/sources.list.d/sbt_old.list && \
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823 && \
    apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y --no-install-recommends sbt=1.2.7 wget

WORKDIR /ergo-faucet

RUN wget https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-19.3.1/graalvm-ce-java8-linux-amd64-19.3.1.tar.gz && \
    tar -xf graalvm-ce-java8-linux-amd64-19.3.1.tar.gz
ENV JAVA_HOME="/ergo-faucet/graalvm-ce-java8-19.3.1"
ENV PATH="${JAVA_HOME}/bin:$PATH"

ADD ["./app", "/ergo-faucet/src/app"]
ADD ["./conf", "/ergo-faucet/src/conf"]
ADD ["./project", "/ergo-faucet/src/project"]
COPY build.sbt /ergo-faucet/src/

WORKDIR /ergo-faucet/src/
COPY --from=builder-front /usr/src/app/build/ ./public/
RUN sbt assembly
RUN mv `find . -name ergo-faucet-*.jar` /ergo-faucet.jar
CMD ["java", "-jar", "/ergo-faucet.jar"]

FROM openjdk:8-jre-slim
RUN adduser --disabled-password --home /home/ergo/ --uid 9052 --gecos "ErgoPlatform" ergo && \
    install -m 0750 -o ergo -g ergo  -d /home/ergo/ergo-faucet
COPY --from=builder_code /ergo-faucet.jar /home/ergo/ergo-faucet.jar
COPY ./conf/application.conf /home/ergo/ergo-faucet/application.conf
RUN chown ergo:ergo /home/ergo/ergo-faucet.jar
USER ergo
EXPOSE 9000
WORKDIR /home/ergo
ENTRYPOINT java -jar -D"config.file"=ergo-faucet/application.conf /home/ergo/ergo-faucet.jar
