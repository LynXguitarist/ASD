package utils;

public enum ProtocolsIds {
	EARGER_PUSH_GOSSIP((short) 200), PLUMTREE((short) 201), FLOOD((short) 202), 
		CYCLON((short) 300), SIMPLE_FULL_MEMBERSHIP((short) 301);

	private short id;

	ProtocolsIds(short id) {
		this.id = id;
	}

	public short getId() {
		return id;
	}

}
