# CLARIFORM

Coming soon: "Painless linting & formatting for Clarity."

Finds and automatically fixes problems in Clarity code.

The initial version has limited functionality, but it's a start:

1. Validate the syntax of a Clarity contract file.
2. Fix confusing indentation and dangling close-parens.

Please take it for a spin and post your feedback on the issue tracker.

## Usage 

Clariform is not yet *painless* but we're getting there. For now,
to build clariform in [Docker](https://www.docker.com/) execute in a terminal:

$ `git clone https://github.com/njordhov/clariform`  
$ `cd clariform`  
$ `docker-compose run install`  

The build generates a file `clariform.js` and opens a console in a 
Docker container, aliasing `clariform` with `node clariform.js`.

Clariform is meant to be called from the command line.
Execute clariform in the console:

$$ `clariform --help`

Output validated file with corrections and formatting:

$$ `clariform src/test/clariform/basic.clar`

Check the code for errors, exiting with a non-zero value on invalid Clarity:

$$ `clariform --check src/test/clariform/invalid.clar`

To exit the console in the Docker container:

$$ `exit`

The installed script can be invoked with docker-compose:

$ `docker-compose run clariform --help`

Note that Docker wisely restricts file access to the home directory.
To allow access to files elsewhere, mount another directory as the 'home' volume:

$ `docker-compose run -v "$PWD/src/test/clariform:/home" clariform basic.clar`

Alternatively, the Docker file access restriction can be bypassed by 
running the script in node from a terminal outside Docker:

$ `node clariform.js --help`

## Development 

Execute from a terminal to start Docker with a development shell
based on [shadow-cljs](https://github.com/thheller/shadow-cljs):

$ `docker-compose run dev`

To run unit testing, execute in the development shell:

$$ `npm run test`

To generate an executable:

$$ `npm run release`


