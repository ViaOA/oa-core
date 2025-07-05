

Create the javadoc so that it is available at:
   https://viaoa.github.io/oa-core/docs/index.html 

use "dev" branch 
run mvn clean
run mvn javadoc:javadoc
  this will write to target/site/apidocs
  
delete ./docs
copy apidocs to ./apidocs
rename ./apidocs ./docs
commit

