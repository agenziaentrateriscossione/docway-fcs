# FCS-DocWay - INSTALLAZIONE


## Prerequisiti

1. _Java8_
2. LibreOffice 5.0 o superiore ( ma inferiore alla 5.3)
3. Tesseract ocr (_richiesta l'installazione da Sistema Operativo per evitare problemi di identificazione del componente da parte di Apache TIKA_)
4. ImageMagick e poppler-utils (necessario per la funzione pdftotext)


## Configurazione

Prima di poter avviare l'applicazione DocWay-FCS occorre configurare i seguenti files:

- _it.tredi.abstract-fcs.properties_ (directory _classes_)
- _TesseractOCRConfig.properties_ (directory _classes/org/apache/tika/parser/ocr_)
- _it.highwaytech.broker.properties_ (directory _classes_)
- _log4j2.xml_ (directory _classes_)
- _docway-fcs_ se ambiente linux, _docway-fcs.bat_ (per avvio manuale) se ambiente windows, _install_32.bat_ _install_amd64.bat_ _install_ia64.bat_ (per istallazione come servizio a seconda del sistema) se ambiente windows (directory _bin_)

__N.B.__: Per maggiori informazioni sulla configurazione si rimanda ai commenti presenti nei file di properties.

### it.tredi.abstract-fcs.properties

- Configurazione dello specifico host FCS (porta, directory di lavoro, timeout di conversione, ecc.)
- Parametri di configurazione di OpenOffice (o LibreOffice) per conversione di files in PDF
- Parametri di configurazione di ImageMagick per conversione di immagini in PDF

### TesseractOCRConfig.properties

- Contiene i parametri di esecuzione di Tesseract definiti sulla libreria Tika (ogni configurazione di questo file di Tika ignora qualsiasi altra configurazione specificata a livello di it.tredi.abstract-fcs.properties)
- Tra i principali parametri da configurare è specificato il timeout (espresso in secondi) che Tika attende prima di killare il processo Tesseract (_timeout_)
- La configurazione di questo file deve ovviamente essere coerente con quella specificata su it.tredi.abstract-fcs.properties (es. un timeout di Tika inferiore a quello specificato su abstract-fcs comporta il kill del processo Tesseract prima del timeout indicato su FCS)

### it.highwaytech.broker.properties

- Configurazione di host, porta e numero connessioni verso eXtraWay
- Eventuale abilitazione dell'integrazione con Elasticsearch (__N.B.__: In caso di abilitazione di Elasticsearch occorre configurare anche il file cache.ccf con i parametri di attivazione di Apache JCF)

### log4j2.xml

- Percorso assoluto al file di logs di FCS (_property.filename_)
- Numero di file di log e dimenzione

### docway-fcs (LINUX)

- File di avvio di DocWay FCS su Linux. Di seguito è riportata la sezione del file che deve __obbligatoriamente__ essere configurata:

```
# N.B.: E' richiesto Java8 per l'esecuzione del processo
JAVA_COMMAND=/usr/bin/java

# Massima dimensione della memoria heap per il FCS
MAXHEAP=-Xmx128m

FCSPID=/tmp/docway-fcs.pid

NICE_LEVEL=10
```

### docway-fcs.bat (WINDOWS)

- File per un avvio da riga di comando in ambiente Windows. Di seguito è riportata la sezione del file che deve __obbligatoriamente__ essere configurata:

```
rem la variabile JVM serve per impostare la java virtual machine che verra' utilizzata per avviare il servizio
rem per avviare da un persorso specifico si puo' settare la variabile come nell'esempio seguente
rem set "JVM=C:\JDKS\64bit\1.8.0_40\bin\java.exe"
rem NOTA BENE e' richiesta una versione di JVM non inferiore alla 1.8.0
set "JVM=java"

rem per settare le opzioni della JVM settare la variabile JVM_OPTS
set JVM_OPTS=-Xmx1024m -Xms1024m
```

### install-32.bat (WINDOWS)

- File per installare come servizio in ambiente Windows a 32 bit. Di seguito è riportata la sezione del file che deve __obbligatoriamente__ essere configurata:

```
set JVM=auto
rem settata con auto ricava la jvm dal registro di windows
rem per impostarla a una jvm specifica occorre settarla al path della jvm.dll
rem set JVM="C:\JDKS\32bit\1.8.0_40\jre\bin\server\jvm.dll"

set xms=1024m
set xmx=1024m
```

### install-amd64.bat (WINDOWS)

- File per installare come servizio in ambiente Windows amd a 64 bit. Di seguito è riportata la sezione del file che deve __obbligatoriamente__ essere configurata:

```
set JVM=auto
rem impostare a una jvm specifica occorre settarla al path della jvm.dll
rem set JVM="C:\JDKS\32bit\1.8.0_40\jre\bin\server\jvm.dll"

set xms=1024m
set xmx=1024m
```

### install-ia64.bat (WINDOWS)

- File per installare come servizio in ambiente Windows ia a 64 bit. Di seguito è riportata la sezione del file che deve __obbligatoriamente__ essere configurata:

```
set JVM=auto
rem impostare a una jvm specifica occorre settarla al path della jvm.dll
rem set JVM="C:\JDKS\32bit\1.8.0_40\jre\bin\server\jvm.dll"

set xms=1024m
set xmx=1024m
```

### uninstall.bat (WINDOWS)

- File per rimuovere il servizio in ambiente Windows.

## Esecuzione

__Linux__

```
bin/docway-fcs {start|stop|status|restart|debug}
```

__Windows - Avvio Manuale__

Spostarsi da prompt nella directory bin e lanciare

```
docway-fcs.bat
```

__Windows - Installazione Servizio__

Spostarsi da prompt nella directory bin e lanciare a seconda del sistema

```
install_32.bat
```
oppure

```
install_amd64.bat
```
oppure

```
install_ia64.bat
```

__Windows - Disinstallazione Servizio__

Spostarsi da prompt nella directory bin e lanciare a seconda del sistema

```
uninstall.bat
```
