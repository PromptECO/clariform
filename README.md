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
- [x] Autofix multi-expression function bodies
- [ ] Github action to validate Clarity contracts in repository

[![GitHub release](https://img.shields.io/github/release/njordhov/clariform.svg)](https://GitHub.com/njordhov/clariform/releases/)
[![Clariform](https://github.com/njordhov/clariform/actions/workflows/main.yml/badge.svg)](https://github.com/njordhov/clariform/actions/workflows/main.yml)

## Usage

Clariform is not yet *painless* but we're getting there.

Please take it for a spin and post your feedback on the issue tracker.

Clariform is available as prebuilt docker image distributed as a 
[github package](https://github.com/njordhov/clariform/pkgs/container/clariform).
Docker will download and run the image for you. If you prefer to clone the repo 
and build your own, see the instructions in [BUILD.md](BUILD.md).

### Prerequisites

[![Docker](https://badgen.net/badge/icon/docker?icon=docker&label)](https://https://docker.com/)

[Docker](https://https://docker.com/) should be [installed](https://docs.docker.com/engine/install/) 
and up running. 

Pull the clariform image (optional):

```
docker create --rm --pull always ghcr.io/njordhov/clariform
```

This will download _clariform_ and prebuild a docker image. It may take some time 
and generates plenty of output. If you don't do it in advance, the image will be
pulled the first time you run clariform.

### Quick Start

Run `clariform` in a docker container:

```
docker run ghcr.io/njordhov/clariform --help
```

Lint a Clarity contract from a URL and format it to output:

```
docker run ghcr.io/njordhov/clariform "https://raw.githubusercontent.com/njordhov/clariform/main/contracts/malformed.clar"
```

To process local contracts [mount the current working directory](https://docs.docker.com/engine/reference/commandline/run/#mount-volume--v---read-only) as `/home` and list the files
as arguments:

```
docker run -v `pwd`:/home ghcr.io/njordhov/clariform *.clar
```

### Troubleshooting

If _clariform_ doesn't work as expected, make sure you are running 
the latest version:

```
docker run ghcr.io/njordhov/clariform --version
```

To run a specific version of clariform, append the version at the end:

```
docker run ghcr.io/njordhov/clariform:v0.1.2 --version
```

To avoid running an older version of clariform, remove all clariform images: 

```
docker rmi --force $(docker images -q ghcr.io/njordhov/clariform)
```

Alternatively, open the _Docker Desktop_ application to inspect or delete containers and images.

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

## Features

The rest of this section assumes there is a `clariform` alias. 
If not, use this in place of "clariform":
 
```
docker run -v `pwd`:/home ghcr.io/njordhov/clariform
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

### Check Validity

Clariform can check whether a contract is valid Clarity code. If invalid, Clarity 
will report the error and exit. Use the `--check` flag to activate validation:

```
clariform --check "https://raw.githubusercontent.com/njordhov/clariform/main/src/test/invalid.clar"
```

There will be no output if the contract is valid. When checking multiple
contracts it will output the name of each contract before validation.

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

Clariform automatically fixes many common errors in Clarity contracts. 
To disable auto-correct and require valid Clarity syntax, add a `--strict` flag:

```
clariform --strict "https://raw.githubusercontent.com/njordhov/clariform/main/contracts/malformed.clar"
```

For formatting with autocorrect _don't_ add a `--strict` flag:

```
clariform "https://raw.githubusercontent.com/njordhov/clariform/main/contracts/malformed.clar"
```

Clariform inserts required whitespace in expressions and between them:

```clarity 
;; invalid clarity: missing whitespace
(*(+ 1 2)(+ 3 4)) 
```
=>
```clarity 
(* (+ 1 2) (+ 3 4))
```

Clariform fixes missing delimiters and incomplete properties in a _record literal_ (aka "tuple"):

```clarity 
;; invalid clarity: missing property value and delimiters
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

Clariform wraps a multi-expression function body in a `begin` form:

```clarity
;; invalid clarity: multiple expressions in function definition
(define-read-only (half (digit int))
  (asserts! (<= 0 digit 9))
  (/ digit 2))
```
=>
```clarity
(define-read-only (half (digit int))
  (begin
    (asserts! (<= 0 digit 9))
    (/ digit 2)))
```

Please [submit a new issue](https://github.com/njordhov/clariform/issues/new)
if you have suggestions about how `clariform` can be improved.
