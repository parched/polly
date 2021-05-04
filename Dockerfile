FROM ubuntu:20.04

RUN apt-get update && apt-get install -y --no-install-recommends \
  build-essential \
  curl \
  git \
  libssl-dev \
  libz-dev \
  netcat \
  openjdk-11-jdk-headless \
  pkg-config \
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

ARG RUST_VERSION=1.51.0
RUN curl -fLO https://static.rust-lang.org/dist/rust-${RUST_VERSION}-x86_64-unknown-linux-gnu.tar.gz \
  && tar -xf rust*.tar.gz \
  && rm rust*.tar.gz \
  && rust*/install.sh \
  && rm -rf rust*


ARG PYTHON_VERSION=3.9
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
  gnupg \
  && . /etc/os-release \
  && printf "deb http://ppa.launchpad.net/deadsnakes/ppa/ubuntu $UBUNTU_CODENAME main\ndeb-src http://ppa.launchpad.net/deadsnakes/ppa/ubuntu $UBUNTU_CODENAME main\n" >> /etc/apt/sources.list \
  && apt-key adv --keyserver keyserver.ubuntu.com --recv-keys F23C5A6CF475977595C89F51BA6932366A755776 \
  && apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
  python${PYTHON_VERSION} \
  && rm -rf /var/lib/apt/lists/*

ENV PEOTRY_VERSION=1.1.6
ENV POETRY_HOME=/opt/poetry
ENV PATH ${PATH}:${POETRY_HOME}/bin
RUN curl -sSL https://raw.githubusercontent.com/python-poetry/poetry/${PEOTRY_VERSION}/get-poetry.py | python - --no-modify-path \
  && poetry completions bash > /etc/bash_completion.d/poetry.bash-completion
