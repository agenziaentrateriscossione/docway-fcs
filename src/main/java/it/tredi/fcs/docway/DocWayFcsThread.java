package it.tredi.fcs.docway;

import java.io.File;
import java.net.Socket;

import it.tredi.fcs.FcsThread;
import it.tredi.fcs.command.FcaCommandExecutor;

public class DocWayFcsThread extends FcsThread {

	public DocWayFcsThread(Socket client) {
		super(client);
	}

	@Override
	public FcaCommandExecutor getFcaCommandExecutor(String id, String[] convTo, String additionalParams, File workDir) throws Exception {
		return new DocWayFcaCommandExecutor(id, convTo, additionalParams, workDir);
	}

}
