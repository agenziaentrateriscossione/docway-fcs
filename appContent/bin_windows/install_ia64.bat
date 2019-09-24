@echo off

rem ##################################################################################################
rem ### Configurazioni script di avvio - INIZIO
rem ##################################################################################################

set JVM=auto
rem settata con auto ricava la jvm dal registro di windows
rem per impostarla a una jvm specifica occorre settarla al path della jvm.dll
rem set JVM="C:\JDKS\32bit\1.8.0_40\jre\bin\server\jvm.dll"

set xms=1024m
set xmx=1024m

rem ##################################################################################################
rem ### Configurazioni script di avvio - FINE
rem ##################################################################################################

rem # --- NO MORE CHANGES AFTER THIS LINE ---

pushd ..
SET INSTDIR=%CD%
popd

echo Valore della variabile CD:%CD%
echo Valore della variabile INSTDIR:%INSTDIR%

set LIBS=%INSTDIR%\lib
set CLASSES=%INSTDIR%\classes

rem echo Valore della variabile JVM:%JVM%
rem echo Valore della variabile JVM_OPTS:%JVM_OPTS%
echo Valore della variabile LIBS:%LIBS%
echo Valore della variabile CLASSES:%CLASSES%

rem Genero il classpath usando tutti i jar nella directory libs
set classpath=%CLASSES%

rem Creare una variabile con lo stesso nome di un parametro e che inizia con PR_ e' equivalente ad impostare il parametro
rem nel caso seguente e' come impostare il parametro --Classpath=
set PR_CLASSPATH=%CLASSES%;%LIBS%/*

echo Valore della variabile PR_CLASSPATH:%PR_CLASSPATH%

commons-daemon\ia64\docway-fcs //IS//"docway-fcs" --DisplayName="docway-fcs" --Description="docway-fcs"^
	 --Jvm=%JVM%^
     --Install="%cd%\commons-daemon\ia64\docway-fcs.exe" --StartMode=jvm --StopMode=jvm^
     --Startup=auto --StartClass=it.tredi.fcs.docway.DocWayFcs --StopClass=it.tredi.fcs.docway.DocWayFcs^
     --StartMethod=main --StartParams=start --StopMethod=stop^
     --LogLevel=DEBUG^
	 --LogPath="%cd%\logs" --LogPrefix=procrun.log^
	 --StartPath="%cd%\startpath"^
     --StdOutput="%cd%\logs\stdout.log" --StdError="%cd%\logs\stderr.log"^
	 --JvmOptions=-Xms%xms% ++JvmOptions=-Xmx%xmx%


commons-daemon\docway-fcs //ES//"docway-fcs"
