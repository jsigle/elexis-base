#!/bin/bash -v
#
# Command-line version of Gerry Weirichs test for a typical elexis workflow
# as described in http://elexis-forum.ch/viewtopic.php?p=1064&sid=061df191cd3da31fce4053e3175311b5
# Implemented on May, 7 by Niklaus Giger
#
export WORK_DIR=/opt/gerry
for j in A B RemoteServer
do
  echo Working for ${WORK_DIR}/$j
  mkdir -p ${WORK_DIR}/$j
  cd ${WORK_DIR}/$j
  hg init
done

echo "[ui]" >>~/.hgrc
echo "username = Erst Meier ernst@example.commit" >>~/.hgrc

cd ${WORK_DIR}/A
svn export https://elexis.svn.sourceforge.net/svnroot/elexis/trunk/BuildElexis
hg add BuildElexis
hg commit -m "Initial import"
hg push ${WORK_DIR}/RemoteServer

cd ${WORK_DIR}/B
hg pull ${WORK_DIR}/RemoteServer
hg update; ls -lrt

cd ${WORK_DIR}/A
echo "; Adding a comment" >> BuildElexis/options.cmd
hg diff
hg commit -m "C1: comment added"
hg push ${WORK_DIR}/RemoteServer

cd ${WORK_DIR}/B
hg pull ${WORK_DIR}/RemoteServer
hg update; ls -lrt
tail BuildElexis/options.cmd

cd ${WORK_DIR}/A
svn export https://elexis.svn.sourceforge.net/svnroot/elexis/trunk/elexis-kernel
hg add elexis-kernel
hg commit -m "C2: Added elexis-kernel"
hg push ${WORK_DIR}/RemoteServer

cd ${WORK_DIR}/B
hg incoming ${WORK_DIR}/B
hg pull --update ${WORK_DIR}/RemoteServer
ls -lrt

diff . ../B
