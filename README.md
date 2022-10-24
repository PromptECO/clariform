# CLARIFORM

"Painless linting & formatting for Clarity."

Finds and automatically fixes problems in Clarity code.

The initial version has limited functionality, but it's a start:

1. Validate the syntax of a Clarity contract file.
2. Fix confusing indentation and dangling close-parens.

Please take it for a spin and post your feedback on the issue tracker.

Coming soon: Github action

## Usage 

Clariform is not yet *painless* but we're getting there. For now,
to build clariform, with [Docker](https://www.docker.com/) running,
execute in a terminal:

$ `git clone https://github.com/njordhov/clariform`  
$ `cd clariform` 
$ `docker-compose build clariform`

Execute the `clariform` image in Docker: 

$ `docker-compose run clariform --help`

Format a mangled but valid Clarity file:

$ `docker-compose run clariform --format=retain src/test/clariform/malformed.clar`
$ `docker-compose run clariform --format=indent src/test/clariform/malformed.clar`
$ `docker-compose run clariform --format=compact src/test/clariform/malformed.clar`

Check whether Clarity code is invalid:

$ `docker-compose run clariform --check src/test/clariform/invalid.clar`

## Console

To open a Clariform docker container console: 
 
$ `docker-compose run console`  

Clariform can be called from the container's console command line:

$$ `clariform --help`

Check the syntax of a Clarity file, exiting with an error when invalid:

$$ `clariform --check src/test/clariform/invalid.clar`

Format validated code to have consistent indentation and parens:

$$ `clariform src/test/clariform/malformed.clar`

To exit the console in the Docker container:

$$ `exit`

## Command Line 

Clariform can alternatively be invoked as a docker task:

$ `docker-compose run clariform --help`

Note that Docker wisely restricts file access to the home directory.
To allow access to files elsewhere, mount another directory as the 'home' volume:

$ `docker-compose run -v "$PWD/src/test/clariform:/home" clariform basic.clar`

### Node Script

The Docker limited file access can be bypassed by generating a script in
the repo and run it in node from a terminal outside Docker:

$ `git clone https://github.com/njordhov/clariform`  
$ `cd clariform`
$ `node install`
$ `docker-compose run install`
$ `node clariform.js --help`

## Development 

Execute from a terminal to start Docker with a development shell
based on [shadow-cljs](https://github.com/thheller/shadow-cljs):

$ `docker-compose run --service-ports repl`

Call clariform just as in the user console:

$$ `clariform --format src/test/clariform/basic.clar`

$$ `clariform --check src/test/clariform/invalid.clar`

To run unit testing, execute in the development shell:

$$ `npm run test`

To generate an executable:

$$ `npm run release`
