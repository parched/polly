FROM ubuntu:20.04

# Common packaging tools
RUN apt-get update && apt-get install -y --no-install-recommends \
  ca-certificates \
  curl \
  git \
  unzip \
  xz-utils \
  && rm -rf /var/lib/apt/lists/*

# Make the default match non-root users
ENV TAR_OPTIONS --no-same-owner

# Rust
ARG RUST_VERSION=1.52.1
RUN curl -fLO https://static.rust-lang.org/dist/rust-${RUST_VERSION}-x86_64-unknown-linux-gnu.tar.gz \
  && tar -xf rust*.tar.gz \
  && rm rust*.tar.gz \
  && rust*/install.sh \
  && rm -rf rust*

# Python
ARG PYTHON_VERSION=3.9
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
  software-properties-common \
  && add-apt-repository -y ppa:deadsnakes/ppa \
  && apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
  python${PYTHON_VERSION}-full \
  && rm -rf /var/lib/apt/lists/*

# GraalVM Native Image
ARG GRAALVM_VERSION=21.1.0
ENV PATH=${PATH}:/opt/graalvm/bin
ENV GRAALVM_HOME=/opt/graalvm
RUN apt-get update && apt-get install -y --no-install-recommends \
  build-essential \
  libz-dev \
  pkg-config \
  zlib1g-dev \
  && rm -rf /var/lib/apt/lists/* \
  && cd /opt \
  && curl -fLO https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-${GRAALVM_VERSION}/graalvm-ce-java11-linux-amd64-${GRAALVM_VERSION}.tar.gz \
  && tar -xf graalvm*.tar.gz \
  && rm graalvm*.tar.gz \
  && mv graalvm* graalvm \
  && gu install native-image
ENV NATIVE_IMAGE_INSTALLED=true

# SBT (for Scala)
ARG SBT_VERSION=1.5.2
ENV PATH=${PATH}:/opt/sbt/bin
ARG SBT_GLOBAL_BASE=/opt/sbt/base
ARG SBT_BOOT_DIR=/opt/sbt/boot
ARG SBT_COURSIER_HOME=/opt/sbt/coursier
ARG SBT_IVY_HOME=/opt/sbt/ivy2
ENV SBT_OPTS="-Dsbt.global.base=$SBT_GLOBAL_BASE -Dsbt.boot.directory=$SBT_BOOT_DIR -Dsbt.coursier.home=$SBT_COURSIER_HOME -Dsbt.ivy.home=$SBT_IVY_HOME"
RUN apt-get update && apt-get install -y --no-install-recommends \
  openjdk-11-jdk-headless \
  && rm -rf /var/lib/apt/lists/* \
  && cd /opt \
  && curl -fLO https://github.com/sbt/sbt/releases/download/v${SBT_VERSION}/sbt-${SBT_VERSION}.tgz \
  && tar -xf sbt*.tgz \
  && rm sbt*.tgz \
  && mkdir -p $SBT_GLOBAL_BASE $SBT_BOOT_DIR $SBT_COURSIER_HOME $SBT_IVY_HOME \
  && sbt sbtVersion \
  && chmod -R go=u $SBT_GLOBAL_BASE $SBT_BOOT_DIR $SBT_COURSIER_HOME $SBT_IVY_HOME

# Karate
ARG KARATE_VERSION=1.0.1
RUN cd /opt \
  && curl -fLO https://github.com/intuit/karate/releases/download/v${KARATE_VERSION}/karate-${KARATE_VERSION}.zip \
  && unzip karate*.zip \
  && rm karate*.zip \
  && mv karate* karate
ENV PATH ${PATH}:/opt/karate

# Poetry
ENV PEOTRY_VERSION=1.1.6
ENV POETRY_HOME=/opt/poetry
ENV PATH=${PATH}:${POETRY_HOME}/bin
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
  python-is-python3 \
  && rm -rf /var/lib/apt/lists/* \
  && curl -sSL https://raw.githubusercontent.com/parched/poetry/patch-1/get-poetry.py | python - --no-modify-path \
  && poetry completions bash > /etc/bash_completion.d/poetry.bash-completion

# Node Version Manager
ARG NVM_VERSION=0.38.0
ARG NVM_DIR=/opt/nvm
RUN mkdir -p $NVM_DIR \
  && curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v${NVM_VERSION}/install.sh | bash \
  && ln -s $NVM_DIR/nvm.sh /etc/profile.d/50-nvm.sh

# Node.js
ARG NODE_VERSION=16
RUN bash -c ". $NVM_DIR/nvm.sh \
    && nvm install $NODE_VERSION \
    && nvm use $NODE_VERSION \
    && nvm alias default $NODE_VERSION"

RUN apt-get update && apt-get install -y --no-install-recommends \
  libssl-dev \
  netcat \
  && rm -rf /var/lib/apt/lists/*
