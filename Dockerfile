## https://hub.docker.com/_/clojure https://github.com/Quantisan/docker-clojure

## TODO: Consider using https://hub.docker.com/r/theasp/clojurescript-nodejs

FROM clojure:latest

RUN apt-get update  ## optional? 
RUN apt-get install -y curl
RUN curl -sL https://deb.nodesource.com/setup_16.x | bash -
RUN apt-get install -y apt-utils
RUN apt-get autoremove -y
RUN apt-get install -y nodejs
RUN node -v

COPY . /app
WORKDIR /app
RUN npm install
RUN pwd
RUN ls
RUN npx shadow-cljs release script
RUN echo 'alias clariform="node /app/clariform.js"' >> ~/.bashrc
RUN echo 'PS1="$\[$\] "' >> ~/.bashrc
RUN ls

# RUN mkdir -p /home
# RUN cp ./clariform.js /home

## WORKDIR /github/workspace

ENTRYPOINT ["node", "/app/clariform.js"]
