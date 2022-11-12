FROM clojure
## https://hub.docker.com/_/clojure 
## https://github.com/Quantisan/docker-clojure

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
RUN npx shadow-cljs release script
RUN echo 'alias clariform="node /app/clariform.js"' >> ~/.bashrc
RUN echo 'PS1="$\[$\] "' >> ~/.bashrc

WORKDIR /home
ENTRYPOINT ["node", "/app/clariform.js"]
