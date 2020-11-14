package utils;

public enum MessageIds {
	CYCLON_MESSAGE(101), CYCLON_MESSAGE_MERGE(102), SAMPLE_MESSAGE(103), I_HAVE_MESSAGE(104), 
		PRUNE_MESSAGE(105), FLOOD_MESSAGE(106), EPG_MESSAGE(107);

	private int id;

	MessageIds(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

}
