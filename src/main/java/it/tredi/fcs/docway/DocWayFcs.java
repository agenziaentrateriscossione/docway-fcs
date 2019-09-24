package it.tredi.fcs.docway;

import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.highwaytech.broker.Broker;
import it.tredi.fcs.Fcs;
import it.tredi.fcs.FcsThread;
import it.tredi.utils.maven.ApplicationProperties;

/**
 * Implementazione di FCS per DocWay (indicizzazione e conversione di file allegati a documenti)
 */
public class DocWayFcs extends Fcs {

	private static final Logger logger = LogManager.getLogger(DocWayFcs.class.getName());
	
	private static final String FCS_ARTIFACTID = "docway-fcs";
	private static final String FCS_GROUPID = "it.tredi";
	
	/**
	 * Metodo MAIN di avvio del processo. Istanzia l'oggetto FCS ed esegue il metodo RUN
	 * @param args
	 */
	public static void main(String[] args) {
		int exitCode = 0;
		try {
			DocWayFcs docwayFcs = new DocWayFcs();
			docwayFcs.run();
			
			logger.info("DocWayFcs.main(): shutdown...");
		}
		catch(Exception e) {
			logger.error("DocWayFcs.main(): got exception... " + e.getMessage(), e);
			exitCode = 1;
		}
		System.exit(exitCode);
	}
	
	private Broker broker;
	
	public DocWayFcs() throws Exception {
		super();
		
		this.broker = new Broker();
	}
	
	@Override
	public String getAppVersion() {
		return ApplicationProperties.getInstance().getVersion(FCS_GROUPID, FCS_ARTIFACTID);
	}

	@Override
	public String getAppBuildDate() {
		return ApplicationProperties.getInstance().getBuildDate(FCS_GROUPID, FCS_ARTIFACTID);
	}

	@Override
	public void onRunException(Exception e) {
	}

	@Override
	public void onRunFinally() {
		invalidateHost();
	}
	
	/**
	 * Chiamata all'invalidate host del broker (eventuale chiusura delle connessioni al client elasticsearch)
	 */
	private void invalidateHost() {
		if (broker != null) {
			try {
				// chiusura delle connessione con l'host eXtraWay (e connessioni dei client elasticsearch)
				broker.invalidateHost();
			} catch (Exception e) {
				logger.error("DocWayFcs.invalidateHost(): got exception on invalidate host... " + e.getMessage(), e);
			}
		}
	}
	
	@Override
	public FcsThread getFcsThread(Socket clientSocket) throws Exception {
		return new DocWayFcsThread(clientSocket);
	}
	
}
