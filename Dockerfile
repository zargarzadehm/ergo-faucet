FROM node:12.14 as builder-front

WORKDIR /usr/src/app
COPY ./ergo-faucet-ui/package.json ./
RUN npm install
COPY ./ergo-faucet-ui ./
RUN npm run build

FROM mozilla/sbt:8u181_1.2.7 as builder
WORKDIR /ergo-faucet

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
