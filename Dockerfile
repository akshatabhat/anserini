# FROM maven:3.6.1-jdk-8-alpine AS builder

# WORKDIR /develop
# RUN  mvn -f /develop/pom.xml clean package appassembler:assemble 

# FROM openjdk:8-alpine

# RUN apk add --no-cache musl-dev linux-headers bash g++ 

# RUN apk --no-cache add python3 python3-dev make cmake

# FROM openjdk:11-jdk-alpine
# FROM adoptopenjdk/openjdk11:alpine-slim
# RUN apk add --no-cache curl tar bash procps

# ENV MAVEN_VERSION 3.3.9

# RUN curl -fsSL http://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz | tar xzf - -C /usr/share \
#   && mv /usr/share/apache-maven-$MAVEN_VERSION /usr/share/maven \
#   && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

# ENV MAVEN_HOME /usr/share/maven

# WORKDIR /develop
# RUN apk --no-cache add python python-dev make cmake bash

# CMD ["mvn clean package appassembler:assemble"]

FROM openjdk:11
RUN apt-get update && apt-get install -y \
  curl \
  wget \
  build-essential \
  maven \
  vim \
&& rm -rf /var/lib/apt/lists/*

RUN wget \
    https://repo.anaconda.com/miniconda/Miniconda3-latest-Linux-x86_64.sh \
    && bash Miniconda3-latest-Linux-x86_64.sh -b \
    && rm -f Miniconda3-latest-Linux-x86_64.sh
    
ENV PATH=/root/miniconda3/bin:$PATH
ENV CONDA_AUTO_UPDATE_CONDA=false

RUN /root/miniconda3/bin/conda create -y --name py37 python=3.7 \
   && /root/miniconda3/bin/conda clean -ya
ENV CONDA_DEFAULT_ENV=py37
ENV CONDA_PREFIX=/root/miniconda3/envs/$CONDA_DEFAULT_ENV
ENV PATH=$CONDA_PREFIX/bin:$PATH

RUN conda install -y -c anaconda pandas
RUN conda install -y -c anaconda pytables

RUN pip install pyserini

WORKDIR /develop

CMD ["mvn clean package appassembler:assemble"]