# CLARIFORM

"Painless linting & formatting for Clarity."

Detects and automatically fixes problems in Clarity code.

This early version has limited functionality, but it's a start:

- [x] Validate the syntax of a Clarity contract file.
- [x] Fix confusing indentation and dangling close-parens.
- [x] Normalize whitespace and decorative tokens.
- [x] Expand record shorthand notation
- [x] Custom formatting of the output code.
- [ ] Process contract from link
- [ ] Github action to validate Clarity contracts in repository

[![GitHub release](https://img.shields.io/github/release/njordhov/clariform.svg)](https://GitHub.com/njordhov/clariform/releases/)
[![Clariform](https://github.com/njordhov/clariform/actions/workflows/main.yml/badge.svg)](https://github.com/njordhov/clariform/actions/workflows/main.yml)

## Usage

Clariform is not yet *painless* but we're getting there.

Please take it for a spin and post your feedback on the issue tracker.

### Prerequisites

[Docker](https://www.docker.com/) should be [installed](https://docs.docker.com/engine/install/) and up running.

### Quick Start

Clariform can be run from a prebuilt docker container distributed as a 
[github package](https://github.com/njordhov/clariform/pkgs/container/clariform):

```
$ docker run ghcr.io/njordhov/clariform:latest --help
```

[Mount the current working directory](https://docs.docker.com/engine/reference/commandline/run/#mount-volume--v---read-only) as `/home` to explicitly give Clariform access to the files in the 
current working directory:

```
$ docker run -v `pwd`:/home ghcr.io/njordhov/clariform:latest contracts/basic.clar
```

### Installation

For convenience and expediency, the prebuilt image can be installed and named:

```
$ docker create --rm --name clariform ghcr.io/njordhov/clariform
```

Now you can run the preloaded container to execute clariform:

```
$ docker run clariform --help
```

Verify you have the latest version:

```
$ docker run clariform --version
```

Docker will by default restrict filesystem access for security.
[Mount the current working directory](https://docs.docker.com/engine/reference/commandline/run/#mount-volume--v---read-only) as `/home` to explicitly give Clariform access to the files in the 
current working directory:

```
$ docker run -v `pwd`:/home clariform
```

This will traverse all Clarity contract files (".clar") in the working directory and either
output an autocorrected indented version, or report a syntax error.

### Create Shortcut

For convenience you may create a shortcut for the `clariform` command. 
On Mac/Unix this may be accomplished by creating an executable script, or just an alias: 

```
$ alias clariform="docker run -v \`pwd\`:/home clariform"
```

Using the shortcut:

```
$ clariform --help
```

### Uninstall

The local docker image can be removed when no longer needed or to reinstall: 

```
$ docker rmi ghcr.io/njordhov/clariform
```

## Features

The rest of this section assumes there is a `clariform` alias. 
If not, use this in place of "clariform":
 
`docker run -v ``pwd``:/home clariform` 

### Select Files

```
$ clariform
```

Filenames and directories can be explicitly specified as arguments:

```
$ clariform *.clar
```

To disable autocorrect and validate correct Clarity syntax, add a `--strict` flag:

```
$ clariform --strict
```

### Format Output

The output formatting can be specified with the `--format` option:

* `indent`: Indent the code for readability while removing other insignificant whitespace.
* `retain`: Keeps the whitespace similar as in the source while balancing dangling end parens.
* `align`: Remove whitespace from the start of every line (uses less space; can be rehydrated with indent).
* `compact`: Collapse each toplevel form into on a single line (useful for code meant to be evaluated by software rather than read by humans). 

Examples:

```
$ clariform --format=indent contracts/malformed.clar
$ clariform --format=retain contracts/malformed.clar   
$ clariform --format=align contracts/malformed.clar  
$ clariform --format=compact contracts/malformed.clar
```

## Usage Alternatives 

Below are alternative ways to run Clariform.
 
### Build from Repo

Build clariform in Docker:

```
$ git clone https://github.com/njordhov/clariform   
$ cd clariform   
$ docker build -t clariform .  
```

Run the clariform image:

```
$ docker run clariform --help
```

Note that Docker by default restricts filesystem access, which benefits security.
[Mount the current working directory](https://docs.docker.com/engine/reference/commandline/run/#mount-volume--v---read-only) as `/home` to explicitly allow access to the filesystem:

```
$ docker run -v `pwd`:/home clariform contracts/malformed.clar
```

As alternative to ``pwd`` use the absolute path of a directory containing Clarity files.
 
### Start with Compose

To build clariform with docker-compose, execute in a terminal:

```
$ git clone https://github.com/njordhov/clariform    
$ cd clariform   
$ docker-compose build clariform  
```

Execute the `clariform` image in Docker: 

```
$ docker-compose run clariform --help
```

Check whether Clarity code is invalid:

```
$ docker-compose run clariform --check src/test/clariform/invalid.clar
```

### Run in Console

To open a Clariform docker container console: 
 
```
$ docker-compose run console  
```

Clariform can be called from the container's console command line:

```
$$ clariform --help
```

Check the syntax of a Clarity file, exiting with an error when invalid:

```
$$ clariform --check src/test/clariform/invalid.clar
```

Format validated code to have consistent indentation and parens:

```
$$ clariform src/test/clariform/malformed.clar
```

To exit the console in the Docker container:

```
$$ exit
```

### Command Line 

To run Clariform as a docker task:

```
$ docker-compose run clariform --help
```

To allow access to files elsewhere, mount another directory as the 'home' volume:

```
$ docker-compose run -v "$PWD/src/test/clariform:/home" clariform basic.clar
```

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

