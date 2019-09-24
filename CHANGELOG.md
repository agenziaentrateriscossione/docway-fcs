# Change Log

## [6.0.17] - 2019-09-10

### Added
- Supporto ad estrazione testo in Arabo tramite OCR da TIFF (Tika + Tesseract)
- Supporto ad estrazione testo da PDF immagine (OCR su PDF)
    - Da attivare su file _PDFParser.properties_
    - Per maggiori informazioni si rimanda alla pagina _README_ del progetto _it.tredi.attachment-text-extractor_

### Changed
- Generazione del file INSTALL.md (estratto dal README) per aggiunta delle sole istruzioni di configurazione nei pacchetti di installazione.


## [6.0.16] - 2019-02-25

### Changed
- Aggiornata la dipendenza del broker per supporto a numero di documenti superiore a 16 milioni. La versione di common è rimasta inalterata per quanto indicato come fix sulla versione 6.0.1 del progetto corrente)


## [6.0.15] - 2019-02-11

### Fixed
- Corretto l'XPath di recupero del file da aggiornare a seguito dell'elaborazione di FCS (estrazione testo, conversioni, etc.) per gestire casi nei quali abbiamo piu' istanze dello stesso fileID (situazione anomala che non si dovrebbe verificare)


## [6.0.14] - 2018-12-21

### Fixed
- Corretta la versione e la data di rilascio dell'applicazione indicati all'avvio sui log


## [6.0.13] - 2018-11-16

### Fixed
- Corretto il formato dell'utente notificato ad eXtraWay per FCS (eliminato l'id del thread)


## [6.0.12] - 2018-11-06

### Fixed
- In caso di cancellazione dell'originale occorre comunque mantenere il der_from sul file contenente il testo estratto (Task #16474)
- In caso di conversione fallita e cancellazione dell'originale abilitata, occorre eliminare l'attributo 'agent.delete' sul file (Task #16474)
- Ogni convert (estrazione testo) su un file sul quale risulta già fatta l'attività in precedenza viene ignorato


## [6.0.11] - 2018-09-25

### Changed
- Aggiunto il file di configurazione di Tesseract per permettere la personalizzazione dei propri parametri di configurazione


## [6.0.10] - 2018-03-14

### Fixed
- Aggiunti prerequisiti al file README.md


## [6.0.9] - 2018-02-21

### Fixed
- Aggiunto path assoluto in filePattern di log4j2 per problemi di rollover


## [6.0.8] - 2018-02-19

### Fixed
- Corretto il comando di avvio di conversione tramite ImageMagick su ambiente Windows


## [6.0.7] - 2018-02-16

### Fixed
- Aggiornata la versione di abstract-fcs per bug in conversione da immagini a PDF 


## [6.0.6] - 2018-02-09

### Fixed
- Corretto bug in controllo dello stato di conversione in caso di timeout su conversione da TIFF a PDF con ImageMagick


## [6.0.5] - 2018-02-08

### Fixed
- Corretta configurazione log4j2 per elminazione di vecchie copie dei file di log (passaggio da configurazione tramite properties a xml)


## [6.0.4] - 2018-01-23

### Fixed
- Controllo sull'effettivo esito dell'operazione di conversione in modo da determinare se occorre eliminare o meno il file originale (se 'agent.delete=true')


## [6.0.3] - 2017-10-25

### Fixed
- Gestione della property DocWay '_deleteImagesAfterPDF_' corrispondente all'attributo _@agent.delete_ di xw:file

### Changed
- Aggiunta la notifica dell'utente FCS in connessione con il server eXtraWay


## [6.0.2] - 2017-10-20

### Fixed
- Corretto script di avvio FCS per ambiente Linux
- Aggiunto prerequisito Tesseract al file README.md (problemi su OCR immagini tramite Apache TIKA)


## [6.0.1] - 2017-09-22

### Changed
- Aggiornata la versione di common4.jar (per ripristino della libreria del configuratore - ver. 1.0.2)

### Fixed
- Corretto bug in caricamento dei parametri di attivazione (lettura sempre da singleton di configurazione)


## [6.0.0] - 2017-09-05

### Added
- Realizzazione dell'implementazione di abstract-fcs specifica per DocWay4
- Possibilità di utilizzare questa versione di FCS sia con sono eXtraWay che con la versione eXtraWay/Elasticsearch
- Conversione di documenti in PDF/A tramite OpenOffice (o LibreOffice)
