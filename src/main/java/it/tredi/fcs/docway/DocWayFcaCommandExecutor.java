package it.tredi.fcs.docway;

import java.io.File;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.json.JSONObject;

import it.highwaytech.apps.generic.Connessione;
import it.highwaytech.apps.generic.Selezione;
import it.highwaytech.apps.generic.XMLDocumento;
import it.highwaytech.apps.generic.XmlDoc;
import it.highwaytech.apps.generic.utils.GenericUtils;
import it.highwaytech.broker.BinData;
import it.tredi.fcs.Fcs;
import it.tredi.fcs.FcsConfig;
import it.tredi.fcs.command.FcaCommandExecutor;
import it.tredi.fcs.entity.ConversionTo;
import it.tredi.fcs.entity.Documento;
import it.tredi.fcs.entity.FileActionState;
import it.tredi.fcs.entity.FileToWork;
import it.tredi.fcs.entity.Metadata;
import it.tredi.utils.maven.ApplicationProperties;
import it.tredi.utils.properties.PropertiesReader;

/**
 * Classe di implementazione per DocWay delle indicizzazioni e conversioni richiesta da FCA
 */
public class DocWayFcaCommandExecutor extends FcaCommandExecutor {

	private static final Logger logger = LogManager.getLogger(DocWayFcs.class.getName());

	private static final String BROKER_PROPERTIES_FILE_NAME = "it.highwaytech.broker.properties";
	
	private static final String XWAY_HOST_PROPERTY_NAME = "host";
	private static final String XWAY_PORT_PROPERY_NAME = "port";
	
	private static final String XWAY_HOST_DEFAULT_VALUE = "127.0.0.1";
	private static final int XWAY_PORT_DEFAULT_VALUE = 4859;
	
	private static final String XWFILE_ATTRIBUTE_ACTION_DONE_VALUE = "done";
	private static final String XWFILE_ATTRIBUTE_ACTION_FAIL_VALUE = "fail";
	private static final String XWFILE_ATTRIBUTE_ACTION_IGNORE_VALUE = "ignore";
	private static final String XWFILE_ATTRIBUTE_ACTION_TODO_VALUE = "yes";
	
	// Nome e codice dell'operatore da settare nel checkin di file da parte di FCS
	private static final String NOME_OPERATORE_FCS = "convertitore";
	private static final String COD_OPERATORE_FCS = "convertitore";
	
	/**
	 * Valore da settare sugli allegati TXT in modo da attivare l'indicizzazione del contenuto su eXtraWay
	 */
	private static final String XWAY_TXT_INDEX_ATTRIBUTE_ACTIVATION_VALUE = "yes";
	
	// mbernardini 12/06/2019 : aggiunto BOM UTF8 per i testi estratti tramite Tika + Tesseract
	// FEFF because this is the Unicode char represented by the UTF-8 byte order mark (EF BB BF).
    private static final String UTF8_BOM = "\uFEFF";
	
	private String dbName = null;
	
	/**
	 * Connessione al server eXtraWay
	 */
	private Connessione connessione = null;
	
	/**
	 * Numero fisico del documento corrente
	 */
	private int currentDocPhysDoc = -1;
	
	/**
	 * Costruttore
	 * @param docId
	 * @param convTo
	 * @param additionalParams
	 * @param workDir
	 * @throws Exception
	 */
	public DocWayFcaCommandExecutor(String docId, String[] convTo, String additionalParams, File workDir) throws Exception {
		super(docId, convTo, additionalParams, workDir);

		JSONObject json = new JSONObject(additionalParams);
		
		// mbernardini 25/07/2015 : lettura dei parametri di connessione ad extraway dal file di properties del broker
		PropertiesReader propertiesReader = new PropertiesReader(BROKER_PROPERTIES_FILE_NAME);
		String host = propertiesReader.getProperty(XWAY_HOST_PROPERTY_NAME, XWAY_HOST_DEFAULT_VALUE);
		int port = propertiesReader.getIntProperty(XWAY_PORT_PROPERY_NAME, XWAY_PORT_DEFAULT_VALUE);
		//String host = json.getString("xway.host");
		//int port = json.getInt("xway.port");
		
		this.dbName = json.getString("xway.db");
		
		if (logger.isDebugEnabled()) {
			logger.debug("DocWayFcaCommandExecutor: xway host = " + host);
			logger.debug("DocWayFcaCommandExecutor: xway port = " + port);
			logger.debug("DocWayFcaCommandExecutor: xway dbName = " + dbName);
		}
		
		this.connessione = new Connessione(host, port);
	}

	@Override
	public boolean saveDocumento(Documento documento) throws Exception {
		boolean saved = false;
		
		try {
			long startTime = System.currentTimeMillis();
			
			// connessione ad extraway
			connessione.connect(dbName);
			
			String fcsUser = "xw." + InetAddress.getLocalHost().getCanonicalHostName() + ".fcs";
			if (logger.isInfoEnabled())
				logger.info("DocWayFcaCommandExecutor.saveDocumento(): notifying user \"" + fcsUser + "\"" + " on connection " + connessione.getConnection());
			this.connessione.notifyUser(fcsUser);
			
			// caricamento del documento in base al physdoc caricato...
			if (logger.isInfoEnabled())
				logger.info("DocWayFcaCommandExecutor.saveDocumento(): load (and lock) document by physdoc " + currentDocPhysDoc + "...");
			
			XmlDoc doc = new XmlDoc("", 0, currentDocPhysDoc, connessione);
			doc.loadAndLock();
			try {
				
				XMLDocumento document = doc.getDocument();
				if (logger.isDebugEnabled())
					logger.debug("DocWayFcaCommandExecutor.saveDocumento(): XML doc =\n" + document.asXML());
				
				String nrecord = document.getRootElement().attributeValue("nrecord", "");
				if (logger.isInfoEnabled())
					logger.info("DocWayFcaCommandExecutor.saveDocumento(): doc.@nrecord = " + nrecord);
				
				// aggiornamento del documento... aggiunta dei file convertiti, testo estratto, metadati
				for (FileToWork fileToWork : documento.getFilesToWork()) {
					if (fileToWork != null) {
						String name = fileToWork.getFileName();
						if (name != null && !name.isEmpty()) {
							// mbernardini 01/02/2019 : aggiunto all'xpath di recupero del documento anche controlli sugli attributi di attivazione di fca/fcs
							// La modifica si e' resa necessaria perche' e' stato individuato un documento con 2 istanze dello stesso file (medesimo id). In questa situazione
							// venivano aggiunti i file derivati alla prima istanza, ma era la seconda che aveva il convert=yes e questo non veniva mai rimosso (fca/fcs processava
							// quindi all'infinito il documento).
							Element xwfileEl = (Element) document.selectSingleNode("/doc//node()[name()='xw:file'][@name='" + name + "'][not(@der_from)][@convert='yes' or @agent.pdf='yes' or @agent.meta='yes']");
							if (xwfileEl != null) {
								String title = xwfileEl.attributeValue("title", "");
								
								boolean agentDelete = xwfileEl.attributeValue("agent.delete", "false").toLowerCase().equals("true");
								
								// indicizzazione del testo estratto
								if (fileToWork.getIndex() != FileActionState.TODO) {
									if (getFileExtension(name).toLowerCase().equals("txt")) {
										// Il file di input corrisponde ad un file TXT. Per l'indicizzazione su eXtraWay e' sufficiente aggiungere
										// l'attributo @index="yes"
										xwfileEl.addAttribute("index", XWAY_TXT_INDEX_ATTRIBUTE_ACTIVATION_VALUE);
									}
									else {
										// Il file di input non e' un TXT. Per l'indicizzazione su eXtraWay carichiamo il der_to TXT contenente il testo
										// estratto dal file di input
										String derToName = addContentText(connessione, document, fileToWork.getOutFileText(), name, title, agentDelete);
										appendDerToName(xwfileEl, derToName);
									}
									
									// chiamata all'indicizzazione del testo estratto su Elasticsearch
									indexOnElasticsearch(connessione, nrecord, name, fileToWork.getOutFileText());
								}
								else {
									logger.warn("DocWayFcaCommandExecutor.saveDocumento(): text extraction on file " + name + " failed?");
								}
								
								// aggiunta di metadati
								if (fileToWork.getMeta() != FileActionState.TODO) {
									addMetadataElement(xwfileEl, fileToWork.getOutMetadata());
									updateXmlState(xwfileEl, "agent.meta", fileToWork.getMeta());
								}
								else {
									logger.warn("DocWayFcaCommandExecutor.saveDocumento(): medatata on file " + name + " failed?");
								}
								
								// mbernardini 19/01/2018 : controllo sull'effettivo esito dell'operazione di conversione in modo da determinare se occorre eliminare
								// o meno il file originale (se 'agent.delete=true')
								boolean conversionDone = true;
								
								// Aggiunta di tutte le conversioni richieste sul file
								Collection<ConversionTo> conversions = fileToWork.getConversionsTo();
								if (conversions != null && !conversions.isEmpty()) {
									for (ConversionTo convTo : conversions) {
										if (convTo != null) {
											
											if (convTo.getState() != FileActionState.TODO) {
												File fileTo = convTo.getOutfile();
												if (fileTo != null && fileTo.exists() && fileTo.isFile()) {
													// File di output della conversione prodotto... tento il cariamento su eXtraWay e setto il risultato sul documento
													String derToName = addFcsFile(document, fileTo, name, title, false, agentDelete);
													appendDerToName(xwfileEl, derToName);
													xwfileEl.addAttribute("agent." + convTo.getExtension().toLowerCase(), (derToName != null && !derToName.isEmpty()) ? XWFILE_ATTRIBUTE_ACTION_DONE_VALUE : XWFILE_ATTRIBUTE_ACTION_FAIL_VALUE);
												}
												else {
													// Conversione non riuscita o ignorata (file di output non presente)... setto l'esito sul documento
													xwfileEl.addAttribute("agent." + convTo.getExtension().toLowerCase(), getXwFileAttributeValueByState(convTo.getState()));
												}
											}
											
											if (convTo.getState() != FileActionState.DONE)
												conversionDone = false;
										}
									}
								}
								
								// eliminazione dell'attributo convert
								Attribute attrConvert = xwfileEl.attribute("convert");
								if (attrConvert != null)
									xwfileEl.remove(attrConvert); 
								
								// Controllo attributo 'agent.delete' su xw:file corrente. In caso di agent.delete = true occorre eliminare il file originale (caso di
								// immagini da convertire in PDF), altrimenti il file originale deve essere mantenuto
								if (agentDelete) {
									// mbernardini 06/11/2018 : in caso di conversione fallita e cancellazione dell'originale abilitata, occorre disabilitare il flag di cancellazione dell'originale
									if (conversionDone) {
										if (logger.isDebugEnabled())
											logger.debug("DocWayFcaCommandExecutor.saveDocumento(): delete xw:file '" + name + "' after conversion...");
										xwfileEl.detach();
									}
									else {
										if (logger.isDebugEnabled())
											logger.debug("DocWayFcaCommandExecutor.saveDocumento(): conversion failed, remove agent.delete for xw:file '" + name + "'...");
										
										Attribute attrAgentDelete = xwfileEl.attribute("agent.delete");
										if (attrAgentDelete != null)
											xwfileEl.remove(attrAgentDelete); 
									}
								}
							}
						}
					}
				}
				
				// salvataggio del documento
				doc.save();
				
				saved = true;
			}
			catch (Exception e) {
				logger.error("DocWayFcaCommandExecutor.saveDocumento(): got exception saving doc " + currentDocPhysDoc + "... " + e.getMessage(), e);
				doc.unlock();
			}
			
			if (logger.isInfoEnabled())
				logger.info("DocWayFcaCommandExecutor.saveDocumento(): doc '" + documento.getId() + "', save result = " + saved + "... tooks " + (System.currentTimeMillis()-startTime) + " millis.");
		}
		finally {
			// rilascio della connessione su extraway
			if (connessione.isConnected())
				connessione.disconnect();
		}
		
		return saved;
	}
	
	/**
	 * Dato lo stato di una elaborazione, restituisce il valore da settare sull'attributo (relativo all'elaborazione) definito su xw:file
	 * @param state
	 * @return
	 */
	private String getXwFileAttributeValueByState(FileActionState state) {
		String attrValue = XWFILE_ATTRIBUTE_ACTION_TODO_VALUE;
		if (state != null) {
	 		if (state == FileActionState.FAIL)
				attrValue = XWFILE_ATTRIBUTE_ACTION_FAIL_VALUE;
			else if (state == FileActionState.DONE)
				attrValue = XWFILE_ATTRIBUTE_ACTION_DONE_VALUE;
			else if (state == FileActionState.IGNORE)
				attrValue = XWFILE_ATTRIBUTE_ACTION_IGNORE_VALUE;
		}
		return attrValue;
	}
	
	/**
	 * Aggiunge all'elemento xw:file passato l'attributo der_to. Nel caso l'attributo sia gia' presente e valorizzato il nuovo id del file derivato viene concatenato
	 * a quello/i gia' presente/i (separandoli da punto e virgola)
	 * @param xwfileEl Elemento xw:file da aggiornare
	 * @param derToName Identificativo del file der_to da aggiungere
	 */
	private void appendDerToName(Element xwfileEl, String derToName) {
		if (xwfileEl != null && derToName != null && !derToName.isEmpty()) {
			String currentDerTo = xwfileEl.attributeValue("der_to", "");
			if (!currentDerTo.isEmpty())
				currentDerTo += ";";
			currentDerTo += derToName.trim();
			xwfileEl.addAttribute("der_to", currentDerTo);
		}
	}
	
	/**
	 * Aggiunta del file der_to TXT contenente il testo estratto da un file allegato al documento. Azione necessaria per l'indicizzazione del contenuto
	 * del file su eXtraWay
	 * @param connessione Connessione con il server eXtraWay
	 * @param document Documento XML che si sta elaborando
	 * @param text Contenuto da registrare (testo estratto dal file)
	 * @param derFromName Identificativo del file dal quale il testo e' stato estratto
	 * @param derFromTitle Titolo del file dal quale il testo e' stato estratto
	 * @param deleteOriginal true se occorre eliminare il file originale (dal quale la conversione e' prodotta), false altrimenti
	 * @return Identificativo del file di destinazione (file TXT caricato) contenente il testo estratto (e utilizzato da eXtraWay per l'indicizzazione). NULL in caso di problemi. 
	 * @throws Exception
	 */
	private String addContentText(Connessione connessione, XMLDocumento document, String text, String derFromName, String derFromTitle, boolean deleteOriginal) throws Exception {
		String derToName = null;
		if (text != null && !text.isEmpty()) {
			derToName = addFcsFile(document, (UTF8_BOM + text).getBytes(StandardCharsets.UTF_8), "txt", derFromName, derFromTitle, true, deleteOriginal);
		}
		return derToName;
	}
	
	/**
	 * Indicizzazione su Elasticsearch di un testo estratto da un file
	 * @param connessione Connessione con il server eXtraWay
	 * @param nrecord Nrecord del documento al quale il file e' allegato
	 * @param fileId Identificativo del file (attributo name) al quale fa riferimento il testo estratto
	 * @param fileContent Testo estratto dal file (contenuto da indicizzare)
	 */
	private void indexOnElasticsearch(Connessione connessione, String nrecord, String fileId, String fileContent) {
		if (fileContent != null && !fileContent.isEmpty() 
												&& connessione.getBroker().isElasticsearchEnabled()) {
			// indicizzazione del testo estratto dall'allegato su Elasticsearch
			try {
				connessione.getBroker().addFileContentOnElasticsearch(connessione.getDb(), nrecord, fileId, fileContent);
			}
			catch(Exception e) {
				logger.error("DocWayFcaCommandExecutor.indexOnElasticsearch(): got exception on Elasticsearch indexing... " + e.getMessage(), e);
			}
		}
	}
	
	/**
	 * Aggiunta di un file prodotto dall'elaborazione su FCS (conversione richiesta o file TXT contenente il testo estratto)
	 * @param document Documento XML al quale allegare i files
	 * @param fileTo Riferimento al file da caricare (conversione o TXT con testo estratto)
	 * @param derFromName Nome (id) del file processato in input
	 * @param derFromTitle Titolo (nome reale) del file processato in input
	 * @param index true se occorre settare il file per l'indicizzazione su eXtraWay (index=yes), false altrimenti
	 * @param deleteOriginal true se occorre eliminare il file originale (dal quale la conversione e' prodotta), false altrimenti
	 * @return Identificativo associato (da eXtraWay) al nuovo file caricato, NULL in caso di problemi
	 * @throws Exception
	 */
	private String addFcsFile(XMLDocumento document, File fileTo, String derFromName, String derFromTitle, boolean index, boolean deleteOriginal) throws Exception {
		
		return addFcsFile(document, FileUtils.readFileToByteArray(fileTo), getFileExtension(fileTo.getName()), derFromName, derFromTitle, index, deleteOriginal);
	}
	
	/**
	 * Aggiunta di un file prodotto dall'elaborazione su FCS (conversione richiesta o file TXT contenente il testo estratto)
	 * @param document Documento XML al quale allegare i files
	 * @param attachment Contenuto del file da caricare
	 * @param derFromName Nome (id) del file processato in input
	 * @param derFromTitle Titolo (nome reale) del file processato in input
	 * @param index true se occorre settare il file per l'indicizzazione su eXtraWay (index=yes), false altrimenti
	 * @param deleteOriginal true se occorre eliminare il file originale (dal quale la conversione e' prodotta), false altrimenti
	 * @return Identificativo associato (da eXtraWay) al nuovo file caricato, NULL in caso di problemi
	 * @throws Exception
	 */
	private String addFcsFile(XMLDocumento document, byte[] attachment, String extensionTo, String derFromName, String derFromTitle, boolean index, boolean deleteOriginal) throws Exception {
		String derToName = null;
		if (attachment != null) {
			String derToTitle = changeFileExtension((derFromTitle != null && !derFromTitle.isEmpty()) ? derFromTitle : derFromName, extensionTo);
			if (logger.isDebugEnabled())
				logger.debug("DocWayFcaCommandExecutor.addFcsFile(): derTo.title = " + derToTitle);
			
			String id = GenericUtils.uploadAttach(attachment, derToTitle, connessione);
			if (logger.isInfoEnabled())
				logger.info("DocWayFcaCommandExecutor.addFcsFile(): file uploaded, assigned id = " + id);
			
			// tutti i file convertiti vanno aggiunti sotto l'elemento files...
			Element filesHolderEl = (Element) document.selectSingleNode("/doc/files");
			if (filesHolderEl == null)
				filesHolderEl = document.getRootElement().addElement("files");
			
			Element xwfile = filesHolderEl.addElement("xw:file");
			xwfile.addAttribute("name", id);
			xwfile.addAttribute("title", derToTitle);
			
			// mbernardini 05/11/2018 : in caso di cancellazione dell'originale occorre comunque mantenere il der_from sul file contenente il testo estratto
			if (!deleteOriginal || extensionTo.equals("txt"))
				xwfile.addAttribute("der_from", derFromName); // l'attributo der_from deve essere apposto solo nel caso in cui il file originale non debba essere rimosso
			if (index)
				xwfile.addAttribute("index", XWAY_TXT_INDEX_ATTRIBUTE_ACTIVATION_VALUE);
			
			// Aggiunta dell'elemento chkin al file appena generato...
			Element chkinElement = xwfile.addElement("chkin");
			chkinElement.addAttribute("operatore", NOME_OPERATORE_FCS);
			chkinElement.addAttribute("cod_operatore", COD_OPERATORE_FCS);
			Date now = new Date();
			chkinElement.addAttribute("data", GenericUtils.getData(now));
			chkinElement.addAttribute("ora", GenericUtils.getOra(now));
			
			derToName = id;
		}
		return derToName;
	}
	
	/**
	 * Cambia l'estensione associata ad un nome file
	 * @param fileName Nome del file per il quale aggiornare l'estensione
	 * @param extensionTo Estensione da settare per il file
	 * @return
	 */
	private String changeFileExtension(String fileName, String extensionTo) {
		if (fileName != null && extensionTo != null && !extensionTo.isEmpty()) {
			int index = fileName.lastIndexOf(".");
			if (index != -1)
				fileName = fileName.substring(0, index);
			fileName = fileName + "." + extensionTo; 
		}
		return fileName;
	}
	
	/**
	 * Aggiunta dell'elemento metadata (con tutti i metadata estratti dal file da parte di FCS) ad un element xw:file 
	 * @param xwfileEl Elemento xw:file
	 * @param metadata Metadati estratti dal file
	 */
	private void addMetadataElement(Element xwfileEl, Metadata metadata) {
		if (metadata != null && metadata.getMeta() != null && !metadata.getMeta().isEmpty()) {
			Element metaEl = xwfileEl.element("metadata");
			if (metaEl == null) { // se i metadata risultano gia' estratti ignoro l'azione (elemento metadata presente)
				metaEl = xwfileEl.addElement("metadata");
				metaEl.addAttribute("parser", "AbstractFCS");
				metaEl.addAttribute("parserVersion", ApplicationProperties.getInstance().getVersion(Fcs.FCS_GROUPID, Fcs.FCS_ARTIFACTID));
				
				for (Map.Entry<String, String> entry : metadata.getMeta().entrySet()) {
					if (entry != null) {
						Element entryEl = metaEl.addElement(cleanMetadataKeyName(entry.getKey()));
						entryEl.addText(entry.getValue());
					}
				}
			}
		}
	}
	
	/**
	 * Ripulisce il nome di una chiave relativa ad un metadato in modo da poterla utilizzare come nome di un
	 * elemento XML
	 * @param keyName
	 * @return
	 */
	private String cleanMetadataKeyName(String keyName) {
		if (keyName != null) {
			keyName = keyName.replaceAll(":", "-");
			keyName = keyName.replaceAll(" ", "_");
		}
		return keyName;
	}
	
	/**
	 * Aggiornamento dello stato di un'azione richiesta all'interno dell'elemento xml relativo all'xw:file (es. agent.meta, agent.pdf)
	 * @param el Elemento xw:file
	 * @param attrStateName Nome dell'attributo da aggiornare
	 * @param state Stato dell'azione su FCS (done, fail, ecc.)
	 */
	private void updateXmlState(Element xwfileEl, String attrStateName, FileActionState state) {
		if (xwfileEl != null && attrStateName != null && !attrStateName.isEmpty()) {
			String value = null;
			if (state == FileActionState.FAIL)
				value = XWFILE_ATTRIBUTE_ACTION_FAIL_VALUE;
			else if (state == FileActionState.DONE)
				value = XWFILE_ATTRIBUTE_ACTION_DONE_VALUE;
			else if (state == FileActionState.IGNORE)
				value = XWFILE_ATTRIBUTE_ACTION_IGNORE_VALUE;
			
			if (value != null)
				xwfileEl.addAttribute(attrStateName, value);
		}
	}

	@Override
	public Documento getDocumento(String id, File workDir) throws Exception {
		Documento documento = null;

		try {
			// connessione ad extraway
			connessione.connect(dbName);
			
			String fcsUser = "xw." + InetAddress.getLocalHost().getCanonicalHostName() + ".fcs";
			if (logger.isInfoEnabled())
				logger.info("DocWayFcaCommandExecutor.getDocumento(): notifying user \"" + fcsUser + "\"" + " on connection " + connessione.getConnection());
			this.connessione.notifyUser(fcsUser);
			
			// caricamento del documento in base ai parametri passati...
			if (logger.isInfoEnabled())
				logger.info("DocWayFcaCommandExecutor.getDocumento(): load document by nrecord " + id + "...");
			
			Selezione selezione = new Selezione(connessione);
			long count = selezione.Search("[/doc/@nrecord]=\"" + id + "\"", "", "", 0, 0);
			if (count == 0 || count > 1)
				throw new Exception("Found " + count + " documents by nrecord " + id);
			
			XmlDoc doc = new XmlDoc(selezione.getSelId(), 0, -1, connessione);
			doc.load();
			
			// memorizzo il numero fisico del documento (mi servira' in fase di salvataggio del documento)
			this.currentDocPhysDoc = doc.getPhysDoc();
			if (logger.isInfoEnabled())
				logger.info("DocWayFcaCommandExecutor.getDocumento(): physdoc = " + currentDocPhysDoc);
			
			XMLDocumento document = doc.getDocument();
			if (logger.isDebugEnabled())
				logger.debug("DocWayFcaCommandExecutor.getDocumento(): XML doc =\n" + document.asXML());
			
			// recupero di tutti i files da elaborare (indicizzazioni e/o conversioni)...
			List<FileToWork> filesToWork = new ArrayList<FileToWork>();
			addXwFilesToWork(connessione, workDir, filesToWork, document, "/doc/files//node()[name()='xw:file'][not(@der_from)][@convert='yes' or @agent.pdf='yes' or @agent.meta='yes']");
			addXwFilesToWork(connessione, workDir, filesToWork, document, "/doc/immagini//node()[name()='xw:file'][not(@der_from)][@convert='yes' or @agent.pdf='yes' or @agent.meta='yes']");
			
			if (!filesToWork.isEmpty()) {
				// sono stati individuati file da elaborare, ritorno l'oggetto con le informazioni necessarie all'elaborazione
				documento = new Documento(id);
				documento.setContent(document); // come contenuto setto tutto l'XML recuperato da eXtraWay
				documento.setFilesToWork(filesToWork);
			}
			else {
				// non sono stati trovati files da elaborare...
				logger.warn("DocWayFcaCommandExecutor.getDocumento(): No 'xw:file' found with '@convert' or '@agent.*' on document " + id);
			}
		}
		finally {
			// rilascio della connessione su extraway
			if (connessione.isConnected())
				connessione.disconnect();
		}

		return documento;
	}

	/**
	 * Recupero di tutti i file allegati al documento che devono essere elaborati da FCS
	 * @param connessione Connessione con il server eXtraWay
	 * @param workDir Directory di lavoro del thread FCS
	 * @param filesToWork Lista di files da elaborare
	 * @param document Documento XML dal quale recuperare i files
	 * @param xwfilesXPath XPath da utilizzare per il recupero dei files
	 */
	private void addXwFilesToWork(Connessione connessione, File workDir, List<FileToWork> filesToWork, XMLDocumento document, String xwfilesXPath) throws Exception {
		if (document != null && xwfilesXPath != null && !xwfilesXPath.isEmpty()) {
			List<?> xwfiles = document.selectNodes(xwfilesXPath);
			if (xwfiles != null && !xwfiles.isEmpty()) {
				boolean image = false;
				if (xwfilesXPath.startsWith("/doc/immagini/"))
					image = true;

				// almeno un file da indicizzare/convertire trovato sul documento...
				for(int i=0; i<xwfiles.size(); i++) {
					Element xwfile = (Element) xwfiles.get(i);
					if (xwfile != null) {
						String name = xwfile.attributeValue("name", "");
						if (logger.isInfoEnabled())
							logger.info("DocWayFcaCommandExecutor.addXwFilesToWork(): evaluate xwfile " + name + "...");

						// recupero (tramite analisi dell'xml del documento) di tutte le azioni da compiere sul file
						String ext = getFileExtension(name);
						
						// in base all'estensione del file di origine e la configurazione di xw:file determina se deve essere estratto
						// il testo contenuto nel file
						boolean index = isIndexEnabled(xwfile, ext, image);
						
						// in base all'estensione del file di origine, devono essere valutate le conversioni richieste (es. agent.pdf)
						boolean convert = isConversionEnabled(xwfile, ext);
						
						Map<String, ConversionTo> conversions = new HashMap<String, ConversionTo>();
						
						// TODO eventuale ciclo su altre tipologie di conversione (agent.*)
						FileActionState convState = (convert) ? FileActionState.TODO : FileActionState.IGNORE;
						boolean agentpdf = xwfile.attributeValue("agent.pdf", "").toLowerCase().equals("yes");
						if (agentpdf) {
							String derTo = xwfile.attributeValue("der_to", "");
							if (derTo.contains(".pdf")) 
								convState = FileActionState.IGNORE; // se il file risulta gia' convertito in PDF ignoro la richiesta
							
							conversions.put("pdf", new ConversionTo("pdf", convState));
						}
						
						// TODO agent.xml
						// TODO agent.collect
					
						// richiesta l'estrazione dei metadati del file
						boolean meta = xwfile.attributeValue("agent.meta", "").toLowerCase().equals("yes");

						// istanzio l'oggetto che identifica il file da elaborare...
						FileToWork fileToWork = new FileToWork(name, index, conversions, meta);
						List<ConversionTo> todosConv = fileToWork.getTodoConversionsTo(); // recupero effettivamente le estensioni per le quali sono richieste conversioni (stato=todo)
						if (index || (todosConv != null && !todosConv.isEmpty()) || meta) {
							// se e' stata richiesta almeno un'azione che comporta la lettura del file procedo con lo
							// scaricamento da eXtraWay, in caso contrario tale attivita' risulta superflua
							try {
								// Scaricamento del file da xway e salvataggio su directory di lavoro
								BinData attachData = connessione.getBroker().getAttachData(connessione.getConnection(), dbName, name);
								fileToWork.setInputFile(writeFile(workDir, name, attachData));
							}
							catch(Exception e) {
								logger.error("DocWayFcaCommandExecutor.addXwFilesToWork(): got exception on xwfile '" + name + "' download... " + e.getMessage(), e);
								fileToWork.setAllFailed(); // in questo caso forzo direttamente a failed tutte le azioni sul file...
							}
						}
						filesToWork.add(fileToWork);
					}
				}
			}
		}
	}

	/**
	 * Scrittura su file system del file recuperato da eXtraWay tramite specifica chiamata al broker
	 * @param parent
	 * @param fileName
	 * @param attachData
	 * @return
	 * @throws Exception
	 */
	private File writeFile(File parent, String fileName, BinData attachData) throws Exception {
		File file = new File(parent, fileName);
		FileOutputStream fos = null;
		try {
			int dataOffset = 0;
			while (attachData.content[dataOffset] != 0) dataOffset++;
			fos = new FileOutputStream(file);
			fos.write(attachData.content, ++dataOffset, attachData.length);
		}
		finally {
			if (fos != null)
				fos.close();
		}
		return file;
	}

	/**
	 * Ritorna l'estensione del file dato il nome (id)
	 * @param filename
	 * @return
	 */
	private String getFileExtension(String filename) {
		String ext = "";
		if (filename != null && !filename.isEmpty()) {
			int index = filename.lastIndexOf(".");
			if (index != -1)
				ext = filename.substring(index+1);
		}
		return ext;
	}

	/**
	 * Ritorna true se l'indicizzazione (estrazione del testo) del file e' abilitata, false altrimenti
	 * @param xwfile Elemento xw:file da analizzare
	 * @param ext Estensione del file da analizzare
	 * @param image true se si tratta di un elemento immagine, false in caso di doc. informatico
	 * @return
	 * @throws Exception
	 */
	private boolean isIndexEnabled(Element xwfile, String ext, boolean image) throws Exception {
		// controllo abilitazione index su host FCS
		boolean valid = FcsConfig.getInstance().getActivationParams().isIndexEnabled();
		if (!valid) {
			if (logger.isDebugEnabled())
				logger.debug("DocWayFcaCommandExecutor.isIndexEnabled(): index disabled on FCS");
		}
		
		// controllo se effettivamente l'estrazione del testo e' richiesta (attributo @convert="yes")
		if (valid) {
			String convert = xwfile.attributeValue("convert", "");
			if (!convert.toLowerCase().equals("yes")) {
				valid = false;
				if (logger.isInfoEnabled())
					logger.info("DocWayFcaCommandExecutor.isIndexEnabled(): xw:file index not required... @convert=" + convert);
			}
		}
		
		// mbernardini 06/11/2018 : controllo su der_to txt gia' presente per il file
		if (valid) {
			String derTo = xwfile.attributeValue("der_to", "").toLowerCase();
			if (derTo.contains(".txt")) {
				valid = false;
				
				if (logger.isInfoEnabled())
					logger.info("DocWayFcaCommandExecutor.isIndexEnabled(): xw:file index ignored... @der_to already contains TXT file: " + derTo);
			}
		}

		// controllo su file size
		if (valid && FcsConfig.getInstance().getActivationParams().getIndexMaxFileSize() > 0) {
			long size = 0;
			try {
				String attrSize = xwfile.attributeValue("size", "");
				if (!attrSize.isEmpty())
					size = Integer.parseInt(attrSize);
			}
			catch(Exception e) {
				logger.error("DocWayFcaCommandExecutor.isIndexEnabled(): parse file size exception... " + e.getMessage());
			}
			if (size > 0 && size < FcsConfig.getInstance().getActivationParams().getIndexMaxFileSize()) {
				if (logger.isInfoEnabled())
					logger.info("DocWayFcaCommandExecutor.isIndexEnabled(): file size limit exceeded... " + size + " > " + FcsConfig.getInstance().getActivationParams().getIndexMaxFileSize());
				valid = false;
			}
		}

		// controllo su validita' dell'estensione
		if (valid) {
			valid = FcsConfig.getInstance().getActivationParams().checkIndexFileExtensionValid(ext);
			if (!valid) {
				if (logger.isInfoEnabled())
					logger.info("DocWayFcaCommandExecutor.isIndexEnabled(): file extension is not valid... " + ext);
			}
		}

		return valid && (!image || FcsConfig.getInstance().getActivationParams().isOcrEnabled());
	}

	/**
	 * Ritorna true se la conversione del file e' abilitata, false altrimenti
	 * @param xwfile Elemento xw:file da analizzare
	 * @param ext Estensione del file da analizzare
	 * @return
	 * @throws Exception
	 */
	private boolean isConversionEnabled(Element xwfile, String ext) throws Exception {
		// controllo abilitazione convert su host FCS
		boolean valid = FcsConfig.getInstance().getActivationParams().isConvertEnabled();
		if (!valid) {
			if (logger.isDebugEnabled())
				logger.debug("DocWayFcaCommandExecutor.isConversionEnabled(): convert disabled on FCS");
		}

		// controllo su file size
		if (valid && FcsConfig.getInstance().getActivationParams().getConvertMaxFileSize() > 0) {
			long size = 0;
			try {
				String attrSize = xwfile.attributeValue("size", "");
				if (!attrSize.isEmpty())
					size = Integer.parseInt(attrSize);
			}
			catch(Exception e) {
				logger.error("DocWayFcaCommandExecutor.isConversionEnabled(): parse file size exception... " + e.getMessage());
			}
			if (size > 0 && size < FcsConfig.getInstance().getActivationParams().getConvertMaxFileSize()) {
				if (logger.isInfoEnabled())
					logger.info("DocWayFcaCommandExecutor.isConversionEnabled(): file size limit exceeded... " + size + " > " + FcsConfig.getInstance().getActivationParams().getConvertMaxFileSize());
				valid = false;
			}
		}

		// controllo su validita' dell'estensione
		if (valid && FcsConfig.getInstance().getActivationParams().getConvertFileTypes() != null && !FcsConfig.getInstance().getActivationParams().getConvertFileTypes().isEmpty()) {
			if (!FcsConfig.getInstance().getActivationParams().getConvertFileTypes().contains(ext)) {
				valid = false;
				if (logger.isInfoEnabled())
					logger.info("DocWayFcaCommandExecutor.isConversionEnabled(): file extension is not valid... " + ext);
			}
		}

		return valid;
	}


}
