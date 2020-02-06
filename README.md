# Sistema di Gestione documentale DocWay comprensivo del Modulo FCS
___
> #### Per l'installazione di DocWay ed eXtraWay si rimanda alla pagina [riuso](https://github.com/agenziaentrateriscossione/riuso)
___
## Modulo per la conversione degli allegati

#### Servizio di estrazione testo e conversione di files per applicativo DocWay.
___
[FCA](https://github.com/agenziaentrateriscossione/docway-fca) (File Conversion Agent) e FCS (File Conversion Service) consistono in due processi che permettono l'**estrazione del testo da files** e la **conversione di files in un differente formato** (es. da DOC a PDF). 

Lo scenario di utilizzo può variare a seconda del carico di lavoro:
- In ambienti di ridotte dimensioni (carico di lavoro non elevato) entrambi i processi possono essere installati (e configurati) sullo stesso server;
- In ambienti con un elevato carico di lavoro (in termini di numero di richieste) è possibile scalare l'attività di estrazione testo e conversione su più server. In questo scenario verrà installato **una e una sola istanza di FCA** (che si occuperà di recuperari i lavori da portare a termine) e **N istanze di FCS** (su differenti server) che si occuperanno di elaborare i file e registrare il risultato dell'attività richiesta.

### Flusso di esecuzione

La logica secondo la quale viene svolta tale attività è descritta di seguito:
- FCA si occupa di recuperare tutti i lavori pendenti (richieste di estrazione e/o conversione) e di gestire la coda di lavori pendenti e la comunicazione con il pool di FCS. All'avvio della comunicazione con ogni istanza di FCS viene inviato un set di informazioni inerenti la configurazione dell'ambiente (timeout da applicare all'elaborazione, eventuali estensioni di file da ignorare, etc.);
- Ogni FCS riceve un lavoro da eseguire da parte di FCA, esegue l'attività richiesta e registra il risultato (caricamento dei file prodotti dalla conversione e/o del testo estratto);
- Al termine dell'elaborazione FCS comunica a FCA l'esito dell'attività in modo da poter ricevere il lavoro successivo.

### Descrizione dei progetti

I progetti di FCA e FCS (progetti _JAVA_) sono stati suddivisi in 2 librerie che corrispondo a:
- Logiche generiche utilizzatibili in differenti ambiti (progetti abstract);
- Implementazioni specifiche per uno scenario (nel nostro caso DocWay).

In base alla struttura appena descritta, è quindi possibile utilizzare le librerie abstract per poter gestire scenari differenti da quello DocWay (è sufficiente realizzare le implementazioni richieste dai progetti abstract). Per maggiori dettagli sull'attività si rimanda alla documentazione specifica dei progetti.

#### FCS

[**it.tredi.abstract-fcs**](https://github.com/agenziaentrateriscossione/abstract-fcs): Elaborazione vera e propria dei files. Logiche di conversione e estrazione testo dai file (integrazione con le varie dipendenze software).

**it.tredi.docway-fcs**: Implementazione per DocWay di FCS (aggiornamento dell'esito dei lavori, registrazione dei file convertiti, indicizzazione del testo contenuto negli allegati del documento, etc.).
___
### Prerequisiti:
1. _Java8_
2. LibreOffice 5.0 o superiore ( ma inferiore alla 5.3)
3. Tesseract ocr (_richiesta l'installazione da Sistema Operativo per evitare problemi di identificazione del componente da parte di Apache TIKA_)
4. ImageMagick e poppler-utils (necessario per la funzione pdftotext)

___
### Installazione

Per dettagli sull'installazione del software (configurazione applicativa e dei servizi) consultare il file [INSTALL.md](INSTALL.md).

___
#### Status del progetto:

- stabile

#### Limitazioni sull'utilizzo del progetto:

Il presente modulo deve essere associato a eXtraWay XML database.

___
#### Detentore del copyright:

Agenzia delle Entrate-Riscossione (ADER)

___
#### Soggetto incaricato del mantenimento del progetto open source:
| 3D Informatica srl |
| :------------------- |
| Via Speranza, 35 - 40068 S. Lazzaro di Savena |
| Tel. 051.450844 - Fax 051.451942 |
| http://www.3di.it |

___
#### Indirizzo e-mail a cui inviare segnalazioni di sicurezza:
tickets@3di.it
