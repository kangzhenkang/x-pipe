package com.ctrip.xpipe.redis.core.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.ctrip.xpipe.simpleserver.AbstractIoAction;
import com.ctrip.xpipe.simpleserver.SocketAware;

/**
 * @author wenchao.meng
 *
 * Aug 26, 2016
 */
public abstract class AbstractRedisAction extends AbstractIoAction implements SocketAware{

	private byte[] OK = "+OK\r\n".getBytes();
	private String line;
	
	private boolean slaveof = false;
	private List<String> slaveOfCommands = new ArrayList<String>();
	
	@Override
	protected Object doRead(InputStream ins) throws IOException {
		
		line = readLine(ins);
		if(line != null){
			line = line.trim().toLowerCase();
		}
		if(logger.isInfoEnabled()){
			logger.info("[doRead]" + getLogInfo() + ":" + line);
		}
		
		if(slaveof && !line.startsWith("$") && !line.startsWith("*")){
			slaveOfCommands.add(line);
		}
		return line;
	}
	
	protected String getLogInfo() {
		return socket.toString();
	}
	
	@Override
	protected void doWrite(OutputStream ous) throws IOException {
		
		if(line == null){
			logger.error("[doWrite]" + line);
			return;
		}
		
		byte []towrite = null;

		if(line.startsWith("replconf")){
			towrite = handleReplconf(line);
		}
		
		if(line.equalsIgnoreCase("PING")){
			towrite = "+PONG\r\n".getBytes();
		}
		
		if(line.equalsIgnoreCase("INFO")){
			String info = getInfo();
			String infoCommand = "$" + info.length() + "\r\n" + info + "\r\n";
			towrite = infoCommand.getBytes();
		}
		
		if(line.equalsIgnoreCase("PUBLISH")){
			towrite = ":1\r\n".getBytes();
		}
		
		if(line.equalsIgnoreCase("setname")){
			towrite = "+OK\r\n".getBytes();
		}
		
		if(line.equalsIgnoreCase("MULTI")){
			towrite = "+OK\r\n".getBytes();
		}
		
		if(line.equalsIgnoreCase("slaveof")){
			towrite = "+QUEUED\r\n".getBytes();
			slaveof = true;
			slaveOfCommands.clear();
			slaveOfCommands.add(line);
		}

		if(slaveof && line.startsWith("*")){
			slaveof = false;
			slaveof(slaveOfCommands);
		}
		
		if(line.equalsIgnoreCase("kill") || line.equalsIgnoreCase("rewrite") ){
			towrite = "+QUEUED\r\n".getBytes();
		}
		
		if(line.equalsIgnoreCase("exec")){
			towrite = OK;
		}
		
		if(line.startsWith("psync")){
			try {
				handlePsync(ous, line);
			} catch (InterruptedException e) {
				logger.error("[handlepsync]", e);
			}
		}
		
		if(towrite != null){
			ous.write(towrite);
			ous.flush();
		}
	}

	protected byte[] handleReplconf(String line) {
		String []sp = line.split("\\s+");
		if(sp[1].equals("ack")){
			return null;
		}
		return OK;
	}

	protected void handlePsync(OutputStream ous, String line) throws IOException, InterruptedException {
		
	}

	protected void slaveof(List<String> slaveOfCommands2) {
		
	}

	protected abstract String getInfo();
}
