# CLARIFORM

"Painless linting & formatting for Clarity."

Detects and automatically fixes problems in Clarity code.

This early version has limited functionality, but it's a start:

- [x] Validate the syntax of a Clarity contract file.
- [x] Fix confusing indentation and dangling close-parens.
- [x] Normalize whitespace and decorative tokens.
- [x] Expand record shorthand notation
- [x] Custom formatting of the output code.
- [x] Process contract from url
- [ ] Github action to validate Clarity contracts in repository

[![GitHub release](https://img.shields.io/github/release/njordhov/clariform.svg)](https://GitHub.com/njordhov/clariform/releases/)
[![Clariform](https://github.com/njordhov/clariform/actions/workflows/main.yml/badge.svg)](https://github.com/njordhov/clariform/actions/workflows/main.yml)

## Usage

Clariform is not yet *painless* but we're getting there.

Please take it for a spin and post your feedback on the issue tracker.

### Prerequisites

[![Docker](https://badgen.net/badge/icon/docker?icon=docker&label)](https://https://docker.com/)

[Docker](https://https://docker.com/) should be [installed](https://docs.docker.com/engine/install/) and up running.

### Quick Start

Clariform can be run from a prebuilt docker container distributed as a 
[github package](https://github.com/njordhov/clariform/pkgs/container/clariform):

```
docker run ghcr.io/njordhov/clariform --help
```

Lint a Clarity contract from a URL and format it to output:

```
docker run ghcr.io/njordhov/clariform "https://raw.githubusercontent.com/njordhov/clariform/main/contracts/malformed.clar"
```

[Mount the current working directory](https://docs.docker.com/engine/reference/commandline/run/#mount-volume--v---read-only) as `/home` to explicitly give Clariform access to the files in the 
current working directory:

```
docker run -v `pwd`:/home ghcr.io/njordhov/clariform *.clar
```

### Troubleshooting

If `clariform` doesn't work as expected, make sure you are running 
[![GitHub release](https://img.shields.io/github/release/njordhov/clariform.svg)](https://GitHub.com/njordhov/clariform/releases/) the latest version:

```
docker run ghcr.io/njordhov/clariform --version
```

To run a specific version of clariform, append the version at the end:

```
docker run ghcr.io/njordhov/clariform:v0.1.2 --version
```

To avoid running an older version of clariform, remove all clariform images: 

```
docker image rm --force $(docker images ghcr.io/njordhov/clariform)
```

If this doesn't resolve your troubles, please report the issue on the 
[issue tracker](https://github.com/njordhov/clariform/issues).

### Installation

For convenience and expediency, the prebuilt image can be installed and named:

```
docker create --rm --name clariform ghcr.io/njordhov/clariform
```

Now you can run the preloaded container to execute clariform:

```
docker run clariform --help
```

Verify you have the latest version:

```
docker run clariform --version
```

Docker will by default restrict filesystem access for security.
[Mount the current working directory](https://docs.docker.com/engine/reference/commandline/run/#mount-volume--v---read-only) as `/home` to explicitly give Clariform access to the files in the 
current working directory:

```
docker run -v `pwd`:/home clariform
```

This will traverse all Clarity contract files (".clar") in the working directory and either
output an autocorrected indented version, or report a syntax error.

### Create Shortcut

For convenience you may create a shortcut for the `clariform` command. 
On Mac/Unix this may be accomplished by creating an executable script, or just an alias: 

```
alias clariform="docker run -v \`pwd\`:/home clariform"
```

Using the shortcut:

```
clariform --help
```

### Usage Alternatives

[USAGE.md](USAGE.md) documents other ways to run Clariform.

## Features

The rest of this section assumes there is a `clariform` alias. 
If not, use this in place of "clariform":
 
```
docker run -v `pwd`:/home clariform
``` 

### Select Files

Clariform can open a contract from a url:

```
clariform "https://raw.githubusercontent.com/njordhov/clariform/main/contracts/malformed.clar"
```

Filenames and directories can be explicitly specified as arguments:

```
clariform *.clar
```

When the input contains multiple contracts, Clariform will concatenate 
the contracts in formatted output, prefixing each with their source location as a comment.

### Format Output

The output formatting can be specified with the `--format` option:

* `retain`: Retain whitespace and formatting from the source.
* `adjust`: Keep formatting from the source while lining up dangling close parens.
* `indent` (default): Nest expressions and collapse dangling close parens.
* `auto`: Autoindent for consistency and readibility.
* `align`: Remove whitespace from the start of every line (uses less space; can be rehydrated with indent).
* `compact`: Collapse each toplevel form into on a single line (useful for code meant to be evaluated by software rather than read by humans). 

Examples:

```
clariform --format=adjust contracts/malformed.clar   
```
```
clariform --format=indent contracts/malformed.clar   
```

### Auto Correct 

Clariform automatically fixes many common syntax errors in Clarity contracts. 
To disable auto-correct and require valid Clarity syntax, add a `--strict` flag:

```
clariform --strict "https://raw.githubusercontent.com/njordhov/clariform/main/contracts/malformed.clar"
```

For formatting with autocorrect, don't add a `--strict` flag.

Clariform can insert required whitespace:

```clarity 
;; invalid syntax
(*(+ 1 2)(+ 3 4)) 
```
=>
```clarity 
(* (+ 1 2) (+ 3 4))
```

Clariform can fix missing delimiters and incomplete properties in a _record literal_ (aka "tuple"):

```clarity 
;; invalid syntax
{name,
 age:5
 address "home"}
```
=>
```clarity 
{name: name,
 age: 5,
 address: "home"}
```


