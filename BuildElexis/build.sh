#! /bin/sh
# $Id: build.sh 3247 2007-10-10 10:49:46Z rgw_ch $
cd rsc/build
#$ANT_HOME/bin/ant "Linux" -Dunplugged=1 
ant "Linux"
cd ../..
