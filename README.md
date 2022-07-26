# CLARIFORM

Coming soon: "Painless linting & formatting for Clarity."

## Usage 

Clariform is meant to be called from the command line.

## Roadmap

- Check syntax of Clarity contract code (strict)
- Format code with indentation
- Correct missing parens
- ...

## Development 

Execute from a terminal to start Docker with a development shell
based on [shadow-cljs](https://github.com/thheller/shadow-cljs):

$ `docker-compose run cljs`

For a CLJS REPL, execute in the development shell:

$ `npm run repl`

To run unit testing:

$ `npm run test`

To generate an executable:

$ `npm run release`

Run the executable from the command line:

$ `node out/clariform.js --help`

Check if a file contains valid Clarity:

$ `node out/clariform.js src/test/clariform/basic.clar`

Fails with an error code on invalid Clarity:

$ `node out/clariform.js src/test/clariform/invalid.clar`
