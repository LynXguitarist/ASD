package utils;

public enum TimersIds {
	CYCLON_INFO_TIMER(101), CYCLON_SAMPLE_TIMER(102), INFO_TIMER(103), SAMPLE_TIMER(104);

	private int id;

	TimersIds(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

}
