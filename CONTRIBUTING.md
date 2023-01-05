# CLARIFORM DEVELOPMENT

This development environment is built on [shadow-cljs](https://github.com/thheller/shadow-cljs) and allows you to live reload code changes into the clariform runtime.

## Starting a Watcher

To start a watcher, which will recompile whenever any code files change, use the following command (you can use `-d` to run it in the background):

```
docker-compose up -d watch
```

You can then open the dashboard in a web browser by running:

```
open http://localhost:9630/dashboard
```

Note that the watcher should be running and ready before you execute any of the commands below.

## Starting the Runtime

To start the development runtime, which will reload and repeat the command whenever there are code changes, run the following command in a terminal:

```
docker-compose run runtime --help
```

If you encounter the error "shadow-cljs: giving up trying to connect," wait a little for the watcher to finish launching, then try again.

Edit and save any project file to trigger recompilation and repeated execution.

Here are additional examples of runtime commands:

```
docker-compose run runtime --format=indent contracts/basic.clar
```

```
docker-compose run runtime --check src/test/clariform/invalid.clar
```

Exit the runtime with CTRL-c

## EVAL IN REPL 

The REPL allows you to evaluate Clojure expressions _within_ the runtime.

Start a _repl_: 

```
docker compose exec -it watch npx shadow-cljs cljs-repl script
```

Evaluate a Clojure expression by typing it after the prompt and hit return:

```
(clariform.core/main "--help")
```

Type CTRL-c to exit the repl.

## REBUILD

You may have to rebuild after changes in dependencies:

```
docker-compose build script
```

## UNIT TESTING 

To run unit testing, execute:

```
docker compose run test
```

Optionally enable automatic testing from the "builds" tab of the shadow browser 
dashboard. The output can be viewed in the Docker Desktop in the logs for the
container.

## GENERATE EXECUTABLE

To generate an executable:

```
docker compose run release
```

If you don't have _node_ installed, fire up a console in docker:

```
docker compose run console
```

Execute the generated executable:

```
node clariform.js --help
```

## FINISH

Detach from the container with a CTRL-p CTRL-q key sequence.

Use the docker desktop or docker from the command line.

## PUBLISH 

The github docker-publish workflow has a dispatch event trigger 
to build a distribution and publish as the latest docker image:

1. Update the distribution version in "package.json" and clariform.core
2. Push the main branch and wait for integration testing to succeed.
3. In the github repo, open Releases (link on right side)
4. Draft a new release (using button) with a version tag to be created on publish.
5. A docker page for the tag is automatically generated in
   https://github.com/njordhov/clariform/pkgs/container/clariform


