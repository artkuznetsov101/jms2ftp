package jmsftp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.jms.JMSException;

import org.apache.commons.vfs2.FileSystemException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FTPThread implements Runnable {
	private static final Logger log = LogManager.getLogger();

	JMSProducer producer;
	String queue;
	boolean isClosed = false;

	public FTPThread(String queue) {
		this.queue = queue;
		producer = new JMSProducer(queue);
	}

	@Override
	public void run() {
		String file;

		while (!isClosed) {
			if (!producer.isConnected) {
				producer.connect();
			}
			if (producer.client.remote == null) {
				producer.connectFTP();
			}

			try {
				if (producer.isConnected == true && (file = producer.client.get()) != null) {
					log.info("ftp2jms -> ftp get: " + file);
					producer.send(new String(Files.readAllBytes(Paths.get(Config.FTP.TEMP_DIR, file))));
					log.info("ftp2jms -> jms [" + queue + "] put: " + file);
					producer.client.delete(Config.FTP.TEMP_DIR, file);
					producer.client.delete(file);
					producer.session.commit();
					log.info("ftp2jms -> jms [" + queue + "] commit: " + file);
				} else {
					try {
						Thread.sleep(Config.COMMON.TIMEOUT);
					} catch (InterruptedException e) {
					}
				}
			} catch (IOException e) {
				Emailer.send("jmsftp error", Main.getStackTrace(e));
			} catch (JMSException e) {
				try {
					producer.session.rollback();
					log.error("ftp2jms -> jms [" + queue + "] rollback: " + e.getMessage());
					try {
						Thread.sleep(Config.COMMON.TIMEOUT);
					} catch (InterruptedException e1) {
					}
				} catch (JMSException ex) {
				}
			}
		}
	}

	public void close() {
		isClosed = true;
		producer.disconnect();
	}
}
