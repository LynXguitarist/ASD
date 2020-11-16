package utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Stats {

	// Key -> UUID Value -> time of creation(ms)
	private static Map<UUID, Long> msgCreated = new HashMap<>();
	// Key -> UUID Value -> time of the last receive of the msg(ms)
	private static Map<UUID, Long> msgSent = new HashMap<>();

	private static long numberSent, numberReceived, numberBytesIn, numberBytesOut;

	public Stats() {
	}

	public Map<UUID, Long> getMsgCreated() {
		return msgCreated;
	}

	/**
	 * Called every time a msg is created to be sent
	 * 
	 * @param UUID
	 */
	public void addMsgCreated(UUID id) {
		long currentTime = System.currentTimeMillis();
		msgCreated.putIfAbsent(id, currentTime);
	}

	public Map<UUID, Long> getMsgSent() {
		return msgSent;
	}

	/**
	 * Called every time a msg is retransmitted
	 * 
	 * @param UUID
	 */
	public void addOrUpdateMsgSent(UUID id) {
		msgSent.put(id, System.currentTimeMillis());
	}

	public void joinMsgCreated(Map<UUID, Long> map) {
		msgCreated.putAll(map);
	}

	public void joinMsgSent(Map<UUID, Long> map) {
		map.forEach((id, time) -> {
			if (msgSent.containsKey(id)) {
				if (msgSent.get(id) < time)
					msgSent.put(id, time);
			} else
				msgSent.put(id, time);
		});
	}

	public long getNumberSent() {
		return numberSent;
	}

	public void setNumberSent(long num) {
		numberSent = num;
	}

	public long getNumberReceived() {
		return numberReceived;
	}

	public void setNumberReceived(long num) {
		numberReceived = num;
	}

	public long getNumberBytesIn() {
		return numberBytesIn;
	}

	public void setNumberBytesIn(long num) {
		numberBytesIn = num;
	}

	public long getNumberBytesOut() {
		return numberBytesOut;
	}

	public void setNumberBytesOut(long num) {
		numberBytesOut = num;
	}

}
