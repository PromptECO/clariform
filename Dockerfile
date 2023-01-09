FROM ghcr.io/njordhov/docker-shadow-cljs

MAINTAINER "terje@in-progress.com"

LABEL org.opencontainers.image.title="Clariform"
LABEL org.opencontainers.image.description="Painless linting & formatting for Clarity"
LABEL org.opencontainers.image.authors="Terje Norderhaug (njordhov)"
LABEL org.opencontainers.image.source="https://github.com/njordhov/clariform"
LABEL org.opencontainers.image.licenses=EPL-2.0

COPY . /app
WORKDIR /app
RUN npm install
RUN npx shadow-cljs release script
RUN echo 'alias clariform="node /app/clariform.js"' >> ~/.bashrc
RUN echo 'PS1="$\[$\] "' >> ~/.bashrc

WORKDIR /home
ENTRYPOINT ["node", "/app/clariform.js"]
