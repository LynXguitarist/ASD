package protocols.network.timers;

import babel.generic.ProtoTimer;

public class CyclonTimer extends ProtoTimer {

	public static final short TIMER_ID = 101;

	public CyclonTimer() {
	        super(TIMER_ID);
	    }

	@Override
	public ProtoTimer clone() {
		return this;
	}
}
