FROM openjdk:8-jre-slim as builder_code
ENV DEBIAN_FRONTEND noninteractive
RUN apt-get update && \
    apt-get install -y --no-install-recommends apt-transport-https apt-utils bc dirmngr gnupg && \
    echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list && \
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823 && \
    # seems that dash package upgrade is broken in Debian, so we hold it's version before update
    echo "dash hold" | dpkg --set-selections && \
    apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y --no-install-recommends sbt wget sed

WORKDIR /ergoPayoutAuto

RUN wget https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-19.3.1/graalvm-ce-java8-linux-amd64-19.3.1.tar.gz && \
    tar -xf graalvm-ce-java8-linux-amd64-19.3.1.tar.gz
ENV JAVA_HOME="/ergoPayoutAuto/graalvm-ce-java8-19.3.1"
ENV PATH="${JAVA_HOME}/bin:$PATH"

ADD ["./app", "/ergoPayoutAuto/src/app"]
ADD ["./conf", "/ergoPayoutAuto/src/conf"]
ADD ["./project", "/ergoPayoutAuto/src/project"]
COPY build.sbt /ergoPayoutAuto/src/

WORKDIR /ergoPayoutAuto/src/
RUN sbt assembly
RUN mv `find . -name ergo-payout-auto-*.jar` /ergo-payout-auto.jar
CMD ["java", "-jar", "/ergo-payout-auto.jar"]

FROM openjdk:8-jre-slim
RUN adduser --disabled-password --home /home/ergo/ --uid 9052 --gecos "ErgoPlatform" ergo && \
    install -m 0750 -o ergo -g ergo  -d /home/ergo/ergoPayoutAuto
COPY --from=builder_code /ergo-payout-auto.jar /home/ergo/ergo-payout-auto.jar
COPY ./conf/application.conf /home/ergo/ergoPayoutAuto/application.conf
RUN chown ergo:ergo /home/ergo/ergo-payout-auto.jar
USER ergo
EXPOSE 9000
WORKDIR /home/ergo
ENTRYPOINT java -jar -D"config.file"=ergoPayoutAuto/application.conf /home/ergo/ergo-payout-auto.jar
