CljChips
-------
CljChips is a [RedstoneChips](http://eisental.github.com/RedstoneChips) circuit library that makes it possible to program new circuits and add minecraft commands using [Clojure](http://clojure.org).

Compile
---------
You need to have Maven installed (http://maven.apache.org).

Once installed run `mvn clean package install` from the project root folder.

Install
-------
- Download and install [RedstoneChips](http://eisental.github.com/RedstoneChips).
- Copy CljChips-0.1-SNAPSHOT.jar and plugins/CljChips into your craftbukkit /plugins folder.

Usage
-----
The chip library adds a `clj` circuit type and a /clj command.
The first sign argument for the clj chip is a clojure script namespace (usually the script filename without the .clj extension).

Contribute
----------
- If you have any feature suggestions, bug reports or other issues, please use the [issue tracker](https://github.com/eisental/CljChips/issues).
- We happily accept contributions. The best way to do this is to fork CljChips on GitHub, add your changes, and then submit a pull request. We'll try it out, and hopefully merge it into CljChips.

Changelog
---------

Jan. 28th, 2014 - First commit.
