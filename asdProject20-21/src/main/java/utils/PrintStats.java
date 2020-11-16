package utils;

import java.io.*;
import java.util.UUID;

public class PrintStats {
	public static void main(String[] args) {
		String userDir = System.getProperty("user.dir");
		String path = userDir + "/AllLogs";

		File[] files = new File(path).listFiles();

		LogStats m = null;
		LogStats m1;
		for (File file : files != null ? files : new File[0]) {
			if (file.isFile()) {
				m = WriteToFile.loadFromFile(file.getPath());
				if (m == null) {
					m = WriteToFile.loadFromFile(file.getPath());
				} else {
					m1 = WriteToFile.loadFromFile(file.getPath());
					if (m1 != null) {
						m.joinMsgCreated(m1.getMsgCreated());
						m.joinMsgSent(m1.getMsgSent());
						m.setNumberMsgReceived(m.getNumberMsgReceived() + m1.getNumberBytesReceived());
						m.setNumberMsgTransmitted(m.getNumberMsgTransmitted() + m1.getNumberMsgTransmitted());
						m.setNumberBytesReceived(m.getNumberBytesReceived() + m1.getNumberBytesReceived());
						m.setNumberBytesTransmitted(m.getNumberBytesTransmitted() + m1.getNumberBytesTransmitted());
					}
				}
			}
		}
		if (m != null)
			printMetrics(m);
	}

	public static void printMetrics(LogStats m) {
		int i = 0;
		int val = 0;
		for (UUID u : m.getMsgCreated().keySet()) {
			if (m.getMsgSent().containsKey(u)) {
				val += m.getMsgSent().get(u) - m.getMsgCreated().get(u);
				i++;
			}
		}
		System.err.println("Median Latency: " + val / i + "ms");
		System.err.println("Total Msg In: " + m.getNumberMsgReceived());
		System.err.println("Total Bytes In: " + m.getNumberBytesReceived());
		System.err.println("Total Msg Out: " + m.getNumberMsgTransmitted());
		System.err.println("Total Bytes Out: " + m.getNumberBytesTransmitted());
		System.err.println("Total Msg Lost: " + (m.getNumberMsgReceived() - m.getNumberMsgTransmitted()));
		System.err.println("Total Bytes Lost: " + (m.getNumberBytesReceived() - m.getNumberBytesTransmitted()));
	}
}

class WriteToFile {

	static LogStats loadFromFile(String path) {

		LogStats st = null;

		try {
			FileInputStream fi = new FileInputStream(new File(path));
			ObjectInputStream oi = new ObjectInputStream(fi);

			st = (LogStats) oi.readObject();

			fi.close();
			oi.close();
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}

		return st;
	}

}