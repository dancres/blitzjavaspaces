blitzjavaspaces
===============

The Blitz JavaSpace Implementation

Since Github doesn't support binary downloads, these can be found over at [Google Code](http://code.google.com/p/blitz-javaspaces/downloads/list).

Use maven to compile and test.

Create an installer or jars using Ant (Maven has inadequate support for Jini's PreferredClassLoader infrastructure) as follows:

ant -f ant_jars.xml installer
ant -f ant_jars.xml jars

These latter steps require that you have a full River distribution installed and available at the specified path.

