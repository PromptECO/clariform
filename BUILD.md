## BUILDING CLARIFORM

Below are alternative ways to run Clariform based on the repo.
 
### Build from Repo

Build clariform in Docker:

```
git clone https://github.com/njordhov/clariform   
cd clariform   
docker build -t clariform .  
```

Run the clariform image:

```
docker run clariform --help
```

Note that Docker by default restricts filesystem access, which benefits security.
[Mount the current working directory](https://docs.docker.com/engine/reference/commandline/run/#mount-volume--v---read-only) as `/home` to explicitly allow access to the filesystem:

```
docker run -v `pwd`:/home clariform contracts/malformed.clar
```

As alternative to ``pwd`` use the absolute path of a directory containing Clarity files.
 
### Start with Compose

To build clariform with docker-compose, execute in a terminal:

```
git clone https://github.com/njordhov/clariform    
cd clariform   
docker-compose build clariform  
```

Execute the `clariform` image in Docker: 

```
docker-compose run clariform --help
```

Check whether Clarity code is invalid:

```
docker-compose run clariform --check src/test/clariform/invalid.clar
```

### Run in Console

To open a Clariform docker container console: 
 
```
docker-compose run console  
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
docker-compose run clariform --help
```

To allow access to files elsewhere, mount another directory as the 'home' volume:

```
docker-compose run -v "$PWD/src/test/clariform:/home" clariform basic.clar
```

### Node Script

The Docker limited file access can be bypassed by generating a script in
the repo and run it in [node](https://nodejs.org/en/) from a terminal outside Docker:

```
git clone https://github.com/njordhov/clariform    
cd clariform  
node install  
docker-compose run install  
node clariform.js --help
```