package jmsftp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.commons.vfs2.FileSystemException;

public class JMSProducer implements ExceptionListener {

	FTPClient client = new FTPClient();

	Connection connection;
	Session session;
	Destination destination;
	MessageProducer producer;
	boolean isConnected = false;

	public JMSProducer() {
		connectFTP();
	}

	public void connectFTP() {
		try {
			client.connect(Config.FTP.TEMP_DIR, Config.FTP.FTP_DIR);
		} catch (FileSystemException e) {
			e.printStackTrace();
		}
	}

	public void connect() {
		System.out.println("ftp2jms ->  jms connect");
		try {
			connection = JMSConnectionFactory.getIBMMQ().createConnection();
			connection.setExceptionListener(this);
			session = connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
			destination = session.createQueue(Config.JMS.SEND_QUEUE_NAME);
			producer = session.createProducer(destination);
			isConnected = true;
		} catch (JMSException e) {
			System.out.println("ftp2jms ->  jms connect exception " + e.getMessage());
			// e.printStackTrace();
		}
	}

	public void disconnect() {
		System.out.println("ftp2jms ->  jms disconnect");
		try {
			if (producer != null)
				producer.close();
			if (session != null)
				session.close();
			if (connection != null)
				connection.close();
		} catch (JMSException e) {
			System.out.println("ftp2jms ->  jms disconnect exception " + e.getMessage());
			// e.printStackTrace();
		}
	}

	public void send(String path, String file) throws JMSException, IOException {
		producer.send(session.createTextMessage(new String(Files.readAllBytes(Paths.get(path, file)))));
	}

	@Override
	public void onException(JMSException e) {
		System.out.println("ftp2jms ->  jms onException: " + e.getMessage());
		// e.printStackTrace();

		disconnect();
		isConnected = false;
	}
}
