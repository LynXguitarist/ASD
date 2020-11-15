package utils;

public enum ProtocolsName {
	EARGER_PUSH_GOSSIP("EAGERPUSHGOSSIP"), PLUMTREE("PLUMTREE"), FLOOD("FLOOD"), CYCLON("CYCLON"),
	SIMPLE_FULL_MEMBERSHIP("SIMPLEFULLMEMBERSHIP");

	private String name;

	ProtocolsName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
