# CLARIFORM

"Painless linting & formatting for Clarity."

Finds and automatically fixes problems in Clarity code.

This early version has limited functionality, but it's a start:

1. Validate the syntax of a Clarity contract file.
2. Fix confusing indentation and dangling close-parens.
3. Custom formatting of the output code.

Coming soon: Github action

## Usage 

Clariform is not yet *painless* but we're getting there.

Please take it for a spin and post your feedback on the issue tracker.

There are several experimental ways to run Clariform, all requiring
[Docker](https://www.docker.com/) to be [installed](https://docs.docker.com/engine/install/)
and up running. 

### Docker

Build clariform in Docker:

$ `git clone https://github.com/njordhov/clariform`   
$ `cd clariform`   
$ `docker build -t clariform .`  

Run the clariform image:

$ `docker run clariform --help`

Note that Docker by default restricts file system access, which benefits security.
[Mount the current working directory](https://docs.docker.com/engine/reference/commandline/run/#mount-volume--v---read-only) as `/home` to explicitly allow access to the files in `src/test/clariform`:

$ `docker run -v ``pwd``:/home clariform src/test/clariform/malformed.clar`

As alternative to ``pwd`` use the absolute path of a directory containing Clarity files.
 
### Compose

To build clariform with docker-compose, execute in a terminal:

$ `git clone https://github.com/njordhov/clariform`    
$ `cd clariform`   
$ `docker-compose build clariform`  

Execute the `clariform` image in Docker: 

$ `docker-compose run clariform --help`

Format a valid but mangled Clarity file:

$ `docker-compose run clariform --format=retain src/test/clariform/malformed.clar`  
$ `docker-compose run clariform --format=indent src/test/clariform/malformed.clar`  
$ `docker-compose run clariform --format=compact src/test/clariform/malformed.clar`  

Check whether Clarity code is invalid:

$ `docker-compose run clariform --check src/test/clariform/invalid.clar`

### Console

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

### Command Line 

To run Clariform as a docker task:

$ `docker-compose run clariform --help`

To allow access to files elsewhere, mount another directory as the 'home' volume:

$ `docker-compose run -v "$PWD/src/test/clariform:/home" clariform basic.clar`

### Node Script

The Docker limited file access can be bypassed by generating a script in
the repo and run it in [node](https://nodejs.org/en/) from a terminal outside Docker:

$ `git clone https://github.com/njordhov/clariform`    
$ `cd clariform`  
$ `node install`  
$ `docker-compose run install`  
$ `node clariform.js --help`  

## Development 

The development environment is based on [shadow-cljs](https://github.com/thheller/shadow-cljs)
providing hot-loading of the recompiled clariform script.

Execute from a terminal to run the development script and implicitly start 
a watcher to recompile the script whenever files are changed:

$ `docker-compose run script --help`

$ `docker-compose run script --format src/test/clariform/basic.clar`

$ `docker-compose run script --check src/test/clariform/invalid.clar`

A dashboard for the watcher is available in a web browser:

http://localhost:9630/dashboard

To run unit testing, execute in the development shell:

$$ `npm run test`

To generate an executable:

$$ `npm run release`
