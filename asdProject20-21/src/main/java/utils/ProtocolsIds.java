package utils;

public enum ProtocolsIds {
	EARGER_PUSH_GOSSIP(200), PLUMTREE(201), FLOOD(202), CYCLON(300), SIMPLE_FULL_MEMBERSHIP(301);

	private int id;

	ProtocolsIds(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

}
