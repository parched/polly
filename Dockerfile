FROM ubuntu:20.04

RUN apt-get update && apt-get install -y --no-install-recommends \
  curl \
  git \
  openjdk-11-jdk-headless \
  unzip \
  xz-utils \
  zip \
  && rm -rf /var/lib/apt/lists/*

# Make the default match non-root users
ENV TAR_OPTIONS --no-same-owner

ARG SBT_VERSION=1.4.8
RUN cd /opt \
  && curl -f -O https://github.com/sbt/sbt/releases/download/v${SBT_VERSION}/sbt-${SBT_VERSION}.tgz \
  && tar -xf sbt*.tgz \
  && rm sbt*.tgz
ENV PATH ${PATH}:/opt/sbt/bin
