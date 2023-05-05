# CLARIFORM DEVELOPMENT

This development environment is built on [shadow-cljs](https://github.com/thheller/shadow-cljs) which supports live reload of code changes into the clariform runtime.

## Starting a Watcher

To start a shadow-cljs watcher, which will recompile whenever any code files change, use the following command (you can use `-d` to run it in the background):

```bash
docker-compose run --rm shadow-cljs watch script
```

Wait until the watch server has completed starting up, then [open the build monitor](http://localhost:9630/builds) in a web browser:

```bash
open http://localhost:9630/builds
```

From the monitor, you can compile the project, control the watch process for live reloading of automatically compiled code changes, run unit testing,
and generate an executable release.

Alternatively, do it from the command line. The watch server should be running and ready before you execute any of the commands below.

## Starting the Runtime

To start the development runtime, which will reload and repeat the command whenever there are code changes, run the following command in a terminal:

```
docker exec -it server node out/runtime.js
```

If you encounter the error "shadow-cljs: giving up trying to connect," wait a little for the watcher to finish launching, then try again.

Edit and save any project file to trigger recompilation and repeated execution.

Here are additional examples of runtime commands:

```
docker exec -it server node out/runtime.js --format=indent contracts/basic.clar
```

```
docker exec -it server node out/runtime.js --check src/test/invalid.clar
```

## EVAL IN REPL 

The REPL allows you to evaluate Clojure expressions _on_ the runtime. 
It requires both the server and a runtime to be running.

Start a _repl_: 

```
docker compose run --rm shadow-cljs cljs-repl script
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

## PUBLISH DISTRIBUTION

The github docker-publish workflow has a dispatch event trigger 
to build a distribution and publish as the latest docker image:

1. Update the distribution version in "package.json" and clariform.core
2. Stage and commit as "Bump version to x.x.x"
3. Push the main branch and wait for integration testing to succeed
4. In the github repo, open Releases (link on right side)
5. Draft a new release (using button) with a version tag to be created on publish
   A docker page for the tag is automatically generated in
   https://github.com/prompteco/clariform/pkgs/container/clariform
7. Under the Actions tab, select the Docker workflow. 
8. Under _Run workflow_ select tag matching the version then hit the Run workflow button to generate a distribution.


