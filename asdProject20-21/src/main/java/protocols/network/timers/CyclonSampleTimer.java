package protocols.network.timers;

import babel.generic.ProtoTimer;

public class CyclonSampleTimer extends ProtoTimer {

	public static final short TIMER_ID = 110;

	public CyclonSampleTimer() {
	        super(TIMER_ID);
	    }

	@Override
	public ProtoTimer clone() {
		return this;
	}
}
