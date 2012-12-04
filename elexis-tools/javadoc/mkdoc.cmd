@echo off
rem $Id: mkdoc.cmd 4826 2008-12-17 16:43:02Z rgw_ch $
rem =========================
set TOP=d:\source\elexis_trunk
rem ==========================

set j=%TOP%\elexis-tools\javadoc\jdoc.cmd
set BASE=http://www.elexis.ch/javadoc

mkdir elexis-utilities
cd elexis-utilities
%JAVA_HOME%\bin\javadoc -quiet -encoding "utf-8" -charset "utf-8" -docencoding "utf-8" -sourcepath %TOP%\elexis-utilities\src -public -subpackages ch
cd ..


mkdir elexis
cd elexis
%JAVA_HOME%\bin\javadoc -quiet -encoding "utf-8" -charset "utf-8" -docencoding "utf-8" -link ..\elexis-utilities -sourcepath %TOP%\elexis\src -classpath %TOP%\elexis-utilities\bin -public  -subpackages ch
cd ..



call %j% elexis-agenda ch
call %j% elexis-artikel-schweiz ch
call %j% elexis-arzttarife-schweiz ch
call %j% elexis-be-connector ch
call %j% elexis-befunde ch
call %j% elexis-bildanzeige ch
call %j% elexis-diagnosecodes-schweiz ch
call %j% elexis-ebanking-schweiz ch
call %j% elexis-eigendiagnosen ch
call %j% elexis-externe-dokumente ch
call %j% elexis-h-net ch
call %j% elexis-icpc ch
call %j% elexis-importer ch
call %j% elexis-importer-aerztekasse ch
call %j% elexis-mail ch
call %j% elexis-medikamente-bag ch
call %j% elexis-nachrichten ch
call %j% elexis-notes ch
call %j% elexis-omnivore ch
call %j% elexis-privatnotizen ch
call %j% iatrix-help-wiki org
call %j% Laborimport-Viollier ch
call %j% OOWrapper ch
call %j% oowrapper3 ch
call %j% SGAM-xChange ch