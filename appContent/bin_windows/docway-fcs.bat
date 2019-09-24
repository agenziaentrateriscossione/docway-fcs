@echo off

rem ##################################################################################################
rem ### Configurazioni script di avvio - INIZIO
rem ##################################################################################################

rem la variabile JVM serve per impostare la java virtual machine che verra' utilizzata per avviare il servizio
rem per avviare da un persorso specifico si puo' settare la variabile come nell'esempio seguente
rem set "JVM=C:\JDKS\64bit\1.8.0_40\bin\java.exe"
rem NOTA BENE e' richiesta una versione di JVM non inferiore alla 1.8.0
set "JVM=java"

rem per settare le opzioni della JVM settare la variabile JVM_OPTS
set JVM_OPTS=-Xmx1024m -Xms1024m

rem ##################################################################################################
rem ### Configurazioni script di avvio - FINE
rem ##################################################################################################

rem # --- NO MORE CHANGES AFTER THIS LINE ---

set LIBS=..\lib\*
set CLASSES=..\classes

rem echo Valore della variabile JVM:%JVM%
rem echo Valore della variabile JVM_OPTS:%JVM_OPTS%
rem echo Valore della variabile LIBS:%LIBS%
rem echo Valore della variabile CLASSES:%CLASSES%

rem Genero il classpath usando tutti i jar nella directory libs
set classpath=%CLASSES%;%LIBS%

echo Valore della variabile CLASSPATH:%CLASSPATH%

rem eseguo la classe java
%JVM% %JVM_OPTS% -cp %CLASSPATH% it.tredi.fcs.docway.DocWayFcs %*

