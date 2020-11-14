package protocols.network.timers;

import babel.generic.ProtoTimer;

public class CyclonInfoTimer extends ProtoTimer {

	public static final short TIMER_ID = 111;

	public CyclonInfoTimer() {
	        super(TIMER_ID);
	    }

	@Override
	public ProtoTimer clone() {
		return this;
	}
}
