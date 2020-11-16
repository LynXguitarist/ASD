package utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

public class Stats {

	// Key -> UUID Value -> time of creation(ms)
	private static Map<UUID, Long> msgCreated = new HashMap<>();
	// Key -> UUID Value -> time of the last receive of the msg(ms)
	private static Map<UUID, Long> msgSent = new HashMap<>();

	private static long numberSent, numberReceived, numberBytesIn, numberBytesOut;

	public static Map<UUID, Long> getMsgCreated() {
		return msgCreated;
	}

	/**
	 * Called every time a msg is created to be sent
	 * 
	 * @param UUID
	 */
	public static void addMsgCreated(UUID id) {
		long currentTime = System.currentTimeMillis();
		msgCreated.putIfAbsent(id, currentTime);
	}

	public static Map<UUID, Long> getMsgSent() {
		return msgSent;
	}

	/**
	 * Called every time a msg is retransmitted
	 * 
	 * @param UUID
	 */
	public static void addOrUpdateMsgSent(UUID id) {
		msgSent.put(id, System.currentTimeMillis());
	}

	public static void joinMsgCreated(Map<UUID, Long> map) {
		msgCreated.putAll(map);
	}

	public static void joinMsgSent(Map<UUID, Long> map) {
		map.forEach((id, time) -> {
			if (msgSent.containsKey(id)) {
				if (msgSent.get(id) < time)
					msgSent.put(id, time);
			} else
				msgSent.put(id, time);
		});
	}

	public static long getNumberSent() {
		return numberSent;
	}

	public static void setNumberSent(long num) {
		numberSent = num;
	}

	public static long getNumberReceived() {
		return numberReceived;
	}

	public static void setNumberReceived(long num) {
		numberReceived = num;
	}

	public static long getNumberBytesIn() {
		return numberBytesIn;
	}

	public static void setNumberBytesIn(long num) {
		numberBytesIn = num;
	}

	public static long getNumberBytesOut() {
		return numberBytesOut;
	}

	public static void setNumberBytesOut(long num) {
		numberBytesOut = num;
	}

	public static String print() {
		String nIn = "\nNumber Msg In: " + numberReceived;
		String bytesIn = "\nNumber Bytes In: " + numberBytesIn;
		String nOut = "\nNumber Msg Out: " + numberSent;
		String bytesOut = "\nNumber Msg Out: " + numberBytesOut;
		String latency = "\nLatency: " + Stats.averageBroadcastLatency();
		String toString = nIn + bytesIn + nOut + bytesOut + latency;
		return toString;
	}

	private static double averageBroadcastLatency() {
		double sum = 0;
		for (Entry<UUID, Long> entry : msgCreated.entrySet()) {
			sum += getDiffTime(entry.getKey());
		}
		return (double) sum / msgCreated.size();
	}

	/**
	 * Gets the diff between the last time a node sent the msg with the time of
	 * creation of the msg
	 * 
	 * @param UUID
	 * @return time
	 */
	private static long getDiffTime(UUID id) {
		return msgSent.get(id) - msgCreated.get(id);
	}

}
