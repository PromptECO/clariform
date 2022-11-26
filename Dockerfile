FROM clojure:tools-deps
# https://github.com/Quantisan/docker-clojure

MAINTAINER "terje@in-progress.com"

LABEL org.opencontainers.image.title="Clariform"
LABEL org.opencontainers.image.description="Painless linting & formatting for Clarity"
LABEL org.opencontainers.image.authors="Terje Norderhaug (njordhov)"
LABEL org.opencontainers.image.source="https://github.com/njordhov/clariform"

RUN apt-get update 
RUN apt-get install -y curl
RUN curl -sL https://deb.nodesource.com/setup_16.x | bash -
RUN apt-get install -y apt-utils
RUN apt-get autoremove -y
RUN apt-get install -y nodejs
RUN node -v

COPY . /app
WORKDIR /app
RUN npm install
RUN npx shadow-cljs release script
RUN echo 'alias clariform="node /app/clariform.js"' >> ~/.bashrc
RUN echo 'PS1="$\[$\] "' >> ~/.bashrc

WORKDIR /home
ENTRYPOINT ["node", "/app/clariform.js"]
