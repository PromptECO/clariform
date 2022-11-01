# CLARIFORM

"Painless linting & formatting for Clarity."

Finds and automatically fixes problems in Clarity code.

This early version has limited functionality, but it's a start:

- [x] Validate the syntax of a Clarity contract file.
- [x] Fix confusing indentation and dangling close-parens.
- [x] Normalize whitespace and decorative tokens.
- [x] Expand record shorthand notation
- [x] Custom formatting of the output code.
- [ ] Github action to validate Clarity contracts in repository

[![Clariform](https://github.com/njordhov/clariform/actions/workflows/main.yml/badge.svg)](https://github.com/njordhov/clariform/actions/workflows/main.yml)

## Usage 

Clariform is not yet *painless* but we're getting there.

Please take it for a spin and post your feedback on the issue tracker.

Below are different ways to run Clariform, most assuming [Docker](https://www.docker.com/) is [installed](https://docs.docker.com/engine/install/) and up running.

### Quick Start

Clariform can be run from a prebuilt container image distributed as a github package:

$ docker run ghcr.io/njordhov/clariform:main --help

The package is published at: https://github.com/njordhov/clariform/pkgs/container/clariform

For convenience, the prebuilt image can be preloaded and named:

$ docker create --rm --name clariform ghcr.io/njordhov/clariform:main 
$ docker run clariform --help

Docker will by default restrict filesystem access.
[Mount the current working directory](https://docs.docker.com/engine/reference/commandline/run/#mount-volume--v---read-only) as `/home` to explicitly give Clariform access to check the files in the current directory:

$ docker run -v `pwd`:/home clariform`

This will traverse all Clarity contract files (".clar") in the working directory and either
output an autocorrected indented version, or report a syntax error. 

Filenames and directories can be explicitly specified as arguments:

$ docker run -v `pwd`:/home clariform *.clar

To disable autocorrect and validate correct Clarity syntax, add a `--strict` flag:

$ docker run -v `pwd`:/home clariform --strict

### Build from Repo

Build clariform in Docker:

$ git clone https://github.com/njordhov/clariform   
$ cd clariform   
$ docker build -t clariform .  

Run the clariform image:

$ docker run clariform --help

Note that Docker by default restricts filesystem access, which benefits security.
[Mount the current working directory](https://docs.docker.com/engine/reference/commandline/run/#mount-volume--v---read-only) as `/home` to explicitly allow access to the filesystem:

$ docker run -v `pwd`:/home clariform src/test/clariform/malformed.clar

As alternative to ``pwd`` use the absolute path of a directory containing Clarity files.
 
### Start with Compose

To build clariform with docker-compose, execute in a terminal:

$ git clone https://github.com/njordhov/clariform    
$ cd clariform   
$ docker-compose build clariform  

Execute the `clariform` image in Docker: 

$ docker-compose run clariform --help

Format a valid but mangled Clarity file:

$ docker-compose run clariform --format=retain src/test/clariform/malformed.clar  
$ docker-compose run clariform --format=indent src/test/clariform/malformed.clar  
$ docker-compose run clariform --format=compact src/test/clariform/malformed.clar  

Check whether Clarity code is invalid:

$ docker-compose run clariform --check src/test/clariform/invalid.clar

### Run in Console

To open a Clariform docker container console: 
 
$ docker-compose run console  

Clariform can be called from the container's console command line:

$$ clariform --help

Check the syntax of a Clarity file, exiting with an error when invalid:

$$ clariform --check src/test/clariform/invalid.clar

Format validated code to have consistent indentation and parens:

$$ clariform src/test/clariform/malformed.clar

To exit the console in the Docker container:

$$ exit

### Command Line 

To run Clariform as a docker task:

$ docker-compose run clariform --help

To allow access to files elsewhere, mount another directory as the 'home' volume:

$ docker-compose run -v "$PWD/src/test/clariform:/home" clariform basic.clar

### Node Script

The Docker limited file access can be bypassed by generating a script in
the repo and run it in [node](https://nodejs.org/en/) from a terminal outside Docker:

```
$ git clone https://github.com/njordhov/clariform    
$ cd clariform  
$ node install  
$ docker-compose run install  
$ node clariform.js --help
```  
