# Introduction #

To put the code in, one apparently needs to install a Subversion (SVN) client locally.
For an MS Windows machine, trying to get "CollabNet Subversion Client v1.6.3"  from here: http://subversion.tigris.org/getting.html#windows

# Details #

**Adding** a whole tree (once-off):
  * svn import src  https://boxer-bayesian-regression.googlecode.com/svn/trunk/src  --username vmenkov   -m "Initial import"

**Deleting** a file:
  * svn del  https://boxer-bayesian-regression.googlecode.com/svn/trunk/boxer/BXRReader.java.bak  https://boxer-bayesian-regression.googlecode.com/svn/trunk/boxer/DataPoint.java.bak --username vmenkov -m "delete bak files"

**Checking out**:
  * svn checkout https://boxer-bayesian-regression.googlecode.com/svn/trunk/ boxer-bayesian-regression --username vmenkov

(If you are not "vmenkov", use your own Google name instead)

That will check out an entire directory src/boxer on your host.

**Committing**:

Once you've made changes to files, you commit the whole tree:
**svn commit src/boxer**

**Adding** a file:

If you have added a new file, e.g. "add" it before committing like this:
