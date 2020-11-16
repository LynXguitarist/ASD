package utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Stats {

	// Key -> UUID Value -> time of creation(ms)
	private static Map<UUID, Long> msgCreated = new HashMap<>();
	// Key -> UUID Value -> time of the last receive of the msg(ms)
	private static Map<UUID, Long> msgSent = new HashMap<>();
	private static long sum = 0;

	private static long numberSent, numberReceived, numberBytesIn, numberBytesOut;

	/**
	 * Called every time a msg is created to be sent
	 * 
	 * @param UUID
	 */
	public static void addMsgCreated(UUID id) {
		long currentTime = System.currentTimeMillis();
		msgCreated.putIfAbsent(id, currentTime);
	}

	/**
	 * Called every time a msg is retransmitted
	 * 
	 * @param UUID
	 */
	public static void addOrUpdateMsgSent(UUID id) {
		msgSent.put(id, System.currentTimeMillis());
	}

	public static void setNumberSent(long num) {
		numberSent = num;
	}

	public static void setNumberReceived(long num) {
		numberReceived = num;
	}

	public static void setNumberBytesIn(long num) {
		numberBytesIn = num;
	}

	public static void setNumberBytesOut(long num) {
		numberBytesOut = num;
	}

	public static String printStats() {
		double avgBroad = ((double) numberReceived / numberSent) * 100;
		String avgBroadcastRel = "Average Broadcast reliability: " + avgBroad;
		String avgBroadcastLat = "\nAverage Broadcast latency:(ms) " + averageBroadcastLatency();
		String totMsgTrans = "\nTotal Messages/Bytes Transmitted: " + numberSent + " | " + numberBytesOut + "(bytes)";
		String totMsgRec = "\nTotal Messages/Bytes Received: " + numberReceived + " | " + numberBytesIn + "(bytes)";
		String stats = avgBroadcastRel + avgBroadcastLat + totMsgTrans + totMsgRec;
		return stats;
	}

	/*--------------------------------------------Private_Methods------------------------------------------*/

	/**
	 * Gets the Average Broadcast Latency -> used for tests
	 * 
	 * @return avg(time)
	 */
	private static long averageBroadcastLatency() {
		msgCreated.forEach((id, time) -> {
			sum += getDiffTime(id);
		});
		return sum / msgCreated.size();
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
