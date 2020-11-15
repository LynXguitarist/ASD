package utils;

import java.util.HashMap;
import java.util.Map;

public class Stats {

	// Key -> UUID Value -> time of creation(ms)
	private static Map<Long, Long> msgCreated = new HashMap<>();
	// Key -> UUID Value -> time of the last receive of the msg(ms)
	private static Map<Long, Long> msgSent = new HashMap<>();
	private static long sum = 0;

	/**
	 * Called every time a msg is created to be sent
	 * 
	 * @param UUID
	 */
	public static void addMsgCreated(long UUID) {
		msgCreated.put(UUID, System.currentTimeMillis());
	}

	/**
	 * Called every time a msg is retransmitted
	 * 
	 * @param UUID
	 */
	public static void addOrUpdateMsgSent(long UUID) {
		msgSent.put(UUID, System.currentTimeMillis());
	}

	/**
	 * Gets the diff between the last time a node sent the msg with the time of
	 * creation of the msg
	 * 
	 * @param UUID
	 * @return time
	 */
	public static long getDiffTime(long UUID) {
		return msgCreated.get(UUID) - msgSent.get(UUID);
	}

	/**
	 * Gets the Average Broadcast Latency -> used for tests
	 * 
	 * @return avg(time)
	 */
	public static long AverageBroadcastLatency() {
		msgCreated.forEach((UUID, time) -> {
			sum += getDiffTime(UUID);
		});
		return sum / msgCreated.size();
	}
}
