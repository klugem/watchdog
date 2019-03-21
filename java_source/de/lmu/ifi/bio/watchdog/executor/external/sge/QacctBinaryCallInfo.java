package de.lmu.ifi.bio.watchdog.executor.external.sge;

import de.lmu.ifi.bio.watchdog.executor.external.BinaryCallInfo;

public class QacctBinaryCallInfo extends BinaryCallInfo {
	public String id;
	public String status;
	
	public QacctBinaryCallInfo(String id, BinaryCallInfo info) {
		this.id = id;
		this.err = info.err;
		this.out = info.out;
		this.args = info.args;
		this.command = info.command;
		this.exit = info.exit;
	}
}
