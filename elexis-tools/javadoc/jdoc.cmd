
rem $Id: jdoc.cmd 4826 2008-12-17 16:43:02Z rgw_ch $

mkdir %1
cd %1

%JAVA_HOME%\bin\javadoc -charset "utf-8" -docencoding "utf-8" -encoding "utf-8" -link ..\elexis -link ..\elexis-utilities -sourcepath %TOP%\%1\src -public -subpackages %2

cd ..

