This is a simple library that defines Objects representing Bags suitable
for AP Trust.  This is a small wrapper on the Java BagIt library to add
APTrust specific requirements.

# Features
* support for producing bags with required AP Trust metadata
* support for taring of bags (requires "tar" to be available on the path)

# Requirements
* java 8
* maven

# Special instructions

A custom version of the LOC java bagit library is required (and included).
To install it:

```
mvn install:install-file -Dfile=lib/bagit-5.0.0-CUSTOM-SNAPSHOT.jar \
        -DgroupId=gov.loc -DartifactId=bagit \
        -Dversion=5.0.0-CUSTOM-SNAPSHOT -Dpackaging=jar
```
