package protocols.network.timers;

import babel.generic.ProtoTimer;
import utils.TimersIds;

public class CyclonInfoTimer extends ProtoTimer {

	public static final short TIMER_ID = TimersIds.CYCLON_INFO_TIMER.getId();

	public CyclonInfoTimer() {
	        super(TIMER_ID);
	    }

	@Override
	public ProtoTimer clone() {
		return this;
	}
}
