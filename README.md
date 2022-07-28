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

Clariform is not yet *painless* but we're getting there. For now,
execute in a terminal to build clariform in [Docker](https://www.docker.com/):

$ `git clone https://github.com/njordhov/clariform`
$ `cd clariform`
$ `docker-compose run install`

The build generates a file `clariform.js` and opens a console in a 
Docker container, aliasing `clariform` with `node clariform.js`.

Clariform is meant to be called from the command line.
Execute clariform in the console:

$$ `clariform --help`

Check if a file contains valid Clarity:

$$ `clariform src/test/clariform/basic.clar`

Fails with an error code on invalid Clarity:

$$ `clariform src/test/clariform/invalid.clar`

To exit the console in the Docker container:

$$ `exit`

Note that Docker restricts file access to the clariform directory.
As a temporary work-around, this security constraint can optionally 
be bypassed by running the script in node from a terminal outside Docker:

$ `node clariform.js --help`

## Development 

Execute from a terminal to start Docker with a development shell
based on [shadow-cljs](https://github.com/thheller/shadow-cljs):

$ `docker-compose run dev`

To run unit testing, execute in the development shell:

$$ `npm run test`

To generate an executable:

$$ `npm run release`


