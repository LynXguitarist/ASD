package utils;

import java.io.File;
import java.util.UUID;

public class PrintStats {
	public static void main(String[] args) {
		String path = args[0];

		File[] files = new File(path).listFiles();
		Stats m = null;
		Stats m1;
		for (File file : files != null ? files : new File[0]) {
			if (file.isFile()) {
				if (m == null)
					m = WriteToFile.loadFromFile(file.getPath());
				else {
					m1 = WriteToFile.loadFromFile(file.getPath());
					if (m1 != null) {
						m.joinMsgCreated(m1.getMsgCreated());
						m.joinMsgSent(m1.getMsgSent());
						m.setNumberReceived(m.getNumberReceived() + m1.getNumberReceived());
						m.setNumberSent(m.getNumberSent() + m1.getNumberSent());
						m.setNumberBytesIn(m.getNumberBytesIn() + m1.getNumberBytesIn());
						m.setNumberBytesOut(m.getNumberBytesOut() + m1.getNumberBytesOut());
					}
				}
			}
		}
		if (m != null)
			printMetrics(m);
	}

	public static void printMetrics(Stats m) {
		int i = 0;
		int val = 0;
		for (UUID u : m.getMsgCreated().keySet()) {
			if (m.getMsgSent().containsKey(u)) {
				val += m.getMsgSent().get(u) - m.getMsgCreated().get(u);
				i++;
			}
		}
		System.err.println("Median Latency: " + val / i + "ms");
		System.err.println("Total Msg In: " + m.getNumberReceived());
		System.err.println("Total Bytes In: " + m.getNumberBytesIn());
		System.err.println("Total Msg Out: " + m.getNumberSent());
		System.err.println("Total Bytes Out: " + m.getNumberBytesOut());
		System.err.println("Total Msg Lost: " + (m.getNumberReceived() - m.getNumberSent()));
		System.err.println("Total Bytes Lost: " + (m.getNumberBytesIn() - m.getNumberBytesOut()));
	}
}