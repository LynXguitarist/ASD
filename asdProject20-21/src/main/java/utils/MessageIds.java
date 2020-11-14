package utils;

public enum MessageIds {
	CYCLON_MESSAGE((short) 101), CYCLON_MESSAGE_MERGE((short) 102), SAMPLE_MESSAGE((short) 103), 
		I_HAVE_MESSAGE((short) 104), PRUNE_MESSAGE((short) 105), FLOOD_MESSAGE((short) 106), 
			EPG_MESSAGE((short) 107);

	private short id;

	MessageIds(short id) {
		this.id = id;
	}

	public short getId() {
		return id;
	}

}
