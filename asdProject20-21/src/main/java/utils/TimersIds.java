package utils;

public enum TimersIds {
	CYCLON_INFO_TIMER((short) 101), CYCLON_SAMPLE_TIMER((short) 102), INFO_TIMER((short) 103), 
		SAMPLE_TIMER((short) 104), BROADCAST_TIMER((short) 105), EXIT_TIMER((short) 106),
			START_TIMER((short) 107), STOP_TIMER((short) 108);

	private short id;

	TimersIds(short id) {
		this.id = id;
	}

	public short getId() {
		return id;
	}

}
