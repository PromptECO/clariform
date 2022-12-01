# CLARIFORM DEVELOPMENT

The development environment is based on [shadow-cljs](https://github.com/thheller/shadow-cljs)
providing hot-loading of the recompiled clariform script.

## START WATCHER

Start a watcher to recompile the script whenever files are changed (optionally 
with `-d` to run in the background):

```
docker-compose up watch
```

Open a dashboard for the watcher from a web browser (if at first you fail, wait and try again):

```
open http://localhost:9630/dashboard
```

Tip: Don't stop the script from the dashboard.

## RUN SCRIPT

Execute from a terminal to run the development script in a loop (with hotloading):

```
docker-compose run script --help
```

Troubleshooting: If it outputs "shadow-cljs: giving up trying to connect", wait 
a little for the watcher to complete launching, then try again running the script. 

Edit and save any project file to trigger recompilation and execution of script.

```
docker-compose run script --format=indent contracts/basic.clar
```

```
docker-compose run script --check src/test/clariform/invalid.clar
```

Exit from the execution loop with CTRL-c

## REBUILD SCRIPT 

You may have to rebuild the script after changes in dependencies:

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

Execute the generated script:

```
node clariform.js --help
```

## FINISH

Detach from the container with a CTRL-p CTRL-q key sequence.

Use the docker desktop or docker from the command line.

## PUBLISH 

The github docker-publish workflow has a workflow dispatch event trigger 
to build a distribution and publish as the latest docker image:

1. Update the distribution version in "package.json" and clariform.core
2. Push the main branch and wait for integration testing to succeed.
3. In the github repo, open Releases (link on right side)
4. Draft a new release (using button) with a version tag to be created on publish.
5. A docker page for the tag is automatically generated in
   https://github.com/njordhov/clariform/pkgs/container/clariform


