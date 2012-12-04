@echo off
echo Aufruf: hl7export.bat export.path export.tag
echo export.path: 
echo Zum Ubersteuern des Augabeverzeichnisses, welches in settings.ini definiert ist (optional)
echo export.tag: 
echo Zum Ubersteuern des Export Tags, welches in settings.ini definiert ist (optional)
echo Starte HL7 Export..
if exist hl7export_error.log del hl7export_error.log
if exist hl7export.old.log del hl7export.old.log
if exist hl7export.log ren hl7export.log hl7export.old.log
rem Bei Windows 64 Bit: "C:\Program Files (x86)\Java\jdk1.6.0_22\bin\java.exe" -jar hl7export-VERSION.jar > hl7export.log 2> hl7export_error.log
rem bei Windows 32 Bit: java.exe -jar hl7export-VERSION.jar > hl7export-VERSION.log 2> hl7export_error.log
java.exe -jar hl7export-VERSION.jar %1 %2 > hl7export.log 2> hl7export_error.log
echo Export beendet. Details in hl7export.log. Exceptions in hl7export_error.log
rem pause