package utils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LogStats implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger logger = LogManager.getLogger(LogStats.class);

	private Long numberMsgTransmitted;
	private Long numberMsgReceived;
	private Long numberBytesTransmitted;
	private Long numberBytesReceived;

	// Key -> UUID Value -> time of creation(ms)
	private Map<UUID, Long> msgCreated = new HashMap<>();
	// Key -> UUID Value -> time of the last receive of the msg(ms)
	private Map<UUID, Long> msgSent = new HashMap<>();

	public LogStats(Long nmt, Long nmr, Long nbt, Long nbr) {
		numberMsgTransmitted = nmt;
		numberMsgReceived = nmr;
		numberBytesTransmitted = nbt;
		numberBytesReceived = nbr;
		logger.info("LogStats created...");
	}

	public LogStats() {
	};

	public Map<UUID, Long> getMsgCreated() {
		return msgCreated;
	}

	public Map<UUID, Long> getMsgSent() {
		return msgSent;
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

	public void setNumberMsgTransmitted(Long numberMsgTransmitted) {
		this.numberMsgTransmitted = numberMsgTransmitted;
	}

	public void setNumberMsgReceived(Long numberMsgReceived) {
		this.numberMsgReceived = numberMsgReceived;
	}

	public void setNumberBytesTransmitted(Long numberBytesTransmitted) {
		this.numberBytesTransmitted = numberBytesTransmitted;
	}

	public void setNumberBytesReceived(Long numberBytesReceived) {
		this.numberBytesReceived = numberBytesReceived;
	}

	public Long getNumberMsgTransmitted() {
		return numberMsgTransmitted;
	}

	public Long getNumberMsgReceived() {
		return numberMsgReceived;
	}

	public Long getNumberBytesTransmitted() {
		return numberBytesTransmitted;
	}

	public Long getNumberBytesReceived() {
		return numberBytesReceived;
	}

}
