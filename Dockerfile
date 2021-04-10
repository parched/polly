FROM ubuntu:20.04

RUN apt-get update && apt-get install -y --no-install-recommends \
  build-essential \
  curl \
  git \
  libz-dev \
  netcat \
  openjdk-11-jdk-headless \
  unzip \
  xz-utils \
  zip \
  zlib1g-dev \
  && rm -rf /var/lib/apt/lists/*

# Make the default match non-root users
ENV TAR_OPTIONS --no-same-owner

ARG SBT_VERSION=1.4.8
RUN cd /opt \
  && curl -fLO https://github.com/sbt/sbt/releases/download/v${SBT_VERSION}/sbt-${SBT_VERSION}.tgz \
  && tar -xf sbt*.tgz \
  && rm sbt*.tgz
ENV PATH ${PATH}:/opt/sbt/bin

ARG KARATE_VERSION=1.0.0
RUN cd /opt \
  && curl -fLO https://github.com/intuit/karate/releases/download/v${KARATE_VERSION}/karate-${KARATE_VERSION}.zip \
  && unzip karate*.zip \
  && rm karate*.zip \
  && mv karate* karate \
  && sed -i -e 's,java -cp.*,java -cp "$(dirname "$0")/karate.jar":. com.intuit.karate.Main "$@",' karate/karate
ENV PATH ${PATH}:/opt/karate

ARG GRAALVM_VERSION=21.0.0.2
ENV PATH ${PATH}:/opt/graalvm/bin
ENV GRAALVM_HOME /opt/graalvm
RUN cd /opt \
  && curl -fLO https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-${GRAALVM_VERSION}/graalvm-ce-java11-linux-amd64-${GRAALVM_VERSION}.tar.gz \
  && tar -xf graalvm*.tar.gz \
  && rm graalvm*.tar.gz \
  && mv graalvm* graalvm \
  && gu install native-image
ENV NATIVE_IMAGE_INSTALLED true
