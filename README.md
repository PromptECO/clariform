# CLARIFORM

"Painless linting & formatting for Clarity."

Clariform is a command-line tool for automatically correcting and formatting 
[Clarity](https://clarity-lang.org/) code.

This early version has limited functionality, but it's a start:

- [x] Validate the syntax of a Clarity contract file
- [x] Fix confusing indentation and dangling close-parens
- [x] Normalize whitespace and decorative tokens
- [x] Expand record shorthand notation
- [x] Custom formatting of the output code
- [x] Process contract from url
- [x] Autofix multi-expression function bodies
- [ ] Distributed as npm package
- [ ] Github action to validate Clarity contracts in a repository

[![GitHub release](https://img.shields.io/github/release/njordhov/clariform.svg)](https://GitHub.com/njordhov/clariform/releases/)
[![Clariform](https://github.com/njordhov/clariform/actions/workflows/main.yml/badge.svg)](https://github.com/njordhov/clariform/actions/workflows/main.yml)

## Usage

While Clariform isn't quite *painless* yet, it's getting there. Please give it a try and let us know what you think on the issue tracker.

Clariform is available as a pre-built Docker image that is distributed as a [GitHub package](https://github.com/njordhov/clariform/pkgs/container/clariform). Docker will download and run the image for you. If you prefer to clone the repository and build your own, see the instructions in [BUILD.md](BUILD.md).

### Prerequisites

[![Docker](https://badgen.net/badge/icon/docker?icon=docker&label)](https://https://docker.com/)

[Docker](https://https://docker.com/) must be [installed](https://docs.docker.com/engine/install/) and running.

Pull the Clariform image (optional):

```
docker pull ghcr.io/njordhov/clariform
```

This will download Clariform and pre-build a Docker image. It may take some time and produce a lot of output. If you don't do this in advance, the image will be pulled the first time you run Clariform.

### Quick Start

Run Clariform in a Docker container:

```
docker run ghcr.io/njordhov/clariform --help
```

Lint a Clarity contract from a URL and format it:

```
docker run ghcr.io/njordhov/clariform "https://raw.githubusercontent.com/njordhov/clariform/main/contracts/malformed.clar"
```

To process local contracts [mount the current working directory](https://docs.docker.com/engine/reference/commandline/run/#mount-volume--v---read-only) as `/home` and list files as arguments:

```
docker run -v `pwd`:/home ghcr.io/njordhov/clariform *.clar
```

This assumes that the current directory contains Clarity contracts with a ".clar" file extension.

The formatted contracts will be written to output.

### Troubleshooting

If Clariform fails to open a file, make sure the directory containing the file is
[mounted](https://docs.docker.com/engine/reference/commandline/run/#mount-volume--v---read-only) and that the pathname of the file is relative to the mount.

If Clariform isn't working as expected, make sure you are running the latest version:

```
docker run ghcr.io/njordhov/clariform --version
```

To run a specific version of clariform, append the version at the end:

```
docker run ghcr.io/njordhov/clariform:v0.4.0 --version
```

To avoid running an older version of clariform, remove all clariform images using this command:

```
docker rmi --force $(docker images -q ghcr.io/njordhov/clariform)
```

Alternatively, open the _Docker Desktop_ application to inspect or delete containers and images.

If these actions don't resolve your issue, please report it on the
[issue tracker](https://github.com/njordhov/clariform/issues).

### Installation

To make it easier to use clariform, create a named container from the prebuilt image:

```
docker pull ghcr.io/njordhov/clariform
docker image tag ghcr.io/njordhov/clariform clariform
```

Run the container to execute clariform:

```
docker run clariform --help
```

Docker restricts filesystem access by default. To give Clariform access
to files, [mount the current working directory](https://docs.docker.com/engine/reference/commandline/run/#mount-volume--v---read-only) as `/home`:

```
docker run -v `pwd`:/home clariform .
```

This will traverse all Clarity contract files (".clar") in the working directory and either output an autocorrected indented version or report a syntax error.

### Create Shortcut

To make it easier to use clariform, you can create a shortcut for the clariform command. 
On Mac/Unix, you can do this by creating an executable script or an alias:
```
alias clariform="docker run -v \`pwd\`:/home clariform"
```

To use the shortcut, run this command:

```
clariform --help
```

## Features

The following instructions assume you have created a clariform alias. 
If you haven't, use this command in place of "clariform":
 
```
docker run -v `pwd`:/home ghcr.io/njordhov/clariform
```

### Select Files

Clariform can open a contract from a URL:

```
clariform "https://raw.githubusercontent.com/njordhov/clariform/main/contracts/malformed.clar"
```

You can specify filenames and directories as arguments:

```
clariform *.clar
```

If the input contains multiple contracts, Clariform will concatenate the 
contracts in formatted output, prefixing each with their source location as a comment.

### Check Validity

Clariform can check if a contract is valid Clarity code. If the contract is invalid, C
larity will report the error and exit.  Use the `--check` flag to activate validation:

```
clariform --check "https://raw.githubusercontent.com/njordhov/clariform/main/src/test/invalid.clar"
```

There will be no output if the contract is valid. When checking multiple
contracts it will output the name of each contract before validation.

### Formatting Options

The --format option allows you to specify the output formatting of Clarity code. 
The following options are available:

* `retain`: Preserves the original whitespace and formatting from the source.
* `adjust`: Keeps the original formatting, but lines up dangling closing parentheses.
* `indent` (default): Nests expressions and collapses dangling closing parentheses.
* `auto`: Auto-indents for consistency and readability.
* `align`: Removes whitespace from the start of each line (uses less space; can be re-indented with the indent option).
* `compact`: Collapses each top-level form into a single line (dense but useful for parsing by other tools).

Here are some examples of using the --format option:

```
clariform --format=adjust contracts/malformed.clar   
```
```
clariform --format=indent contracts/malformed.clar   
```

### Validate Syntax 

To validate that the contracts have strict Clarity syntax, add a `--strict` flag:

```
clariform --strict "https://raw.githubusercontent.com/njordhov/clariform/main/contracts/malformed.clar"
```

### Auto Correct 

Clariform automatically fixes many common errors in Clarity contracts. You can use this to keep your contract files valid, or as a preprocessor to allow shorthand notations and more relaxed syntax during editing.

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

Clariform escapes [unicode](https://home.unicode.org/) glyphs and ensures unicode/utf8 strings are prefixed:

```clarity 
"A special 🎁 for you"
```
=>
```clarity 
u"A special \u{1F381} for you"
```

Clariform formats `let` bindings according to best practices (disabled when `format` option is `retain` or `adjust`):

```clarity
;; Confusing formatting making the binding resemble a function call
(let (
  (foo (+ n 1))
     )
  foo)
```
=>
```clarity 
(let ((foo (+ n 1)))
  foo)
```

Please [submit a new issue](https://github.com/njordhov/clariform/issues/new)
if you have suggestions for improvements.
