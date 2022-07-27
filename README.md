# CLARIFORM

Coming soon: "Painless linting & formatting for Clarity."

The initial version only validates the syntax of a Clarity contract file,
but it's a start... stay tuned.

## Roadmap

√ Validate the syntax of Clarity contract code

- Format code with indentation
- Correct missing parens
- ...

## Usage 

Clariform is meant to be called from the command line.

Execute from a terminal to build clariform in [Docker](https://www.docker.com/):

$ `docker-compose run build`

The build opens a console in Docker aliasing `clariform` with `node out/clariform.js`.

Run the `clariform` executable from the console:

/home# `clariform --help`

Check if a file contains valid Clarity:

/home# `clariform src/test/clariform/basic.clar`

Fails with an error code on invalid Clarity:

/home# `clariform src/test/clariform/invalid.clar`

## Development 

Execute from a terminal to start Docker with a development shell
based on [shadow-cljs](https://github.com/thheller/shadow-cljs):

$ `docker-compose run dev`

To run unit testing, execute in the development shell:

/home# `npm run test`

To generate an executable:

/home# `npm run release`


