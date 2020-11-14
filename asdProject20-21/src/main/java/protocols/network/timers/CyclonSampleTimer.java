package protocols.network.timers;

import babel.generic.ProtoTimer;
import utils.TimersIds;

public class CyclonSampleTimer extends ProtoTimer {

	public static final short TIMER_ID = TimersIds.CYCLON_SAMPLE_TIMER.getId();

	public CyclonSampleTimer() {
	        super(TIMER_ID);
	    }

	@Override
	public ProtoTimer clone() {
		return this;
	}
}
