package protocols.membership.full.timers;

import babel.generic.ProtoTimer;
import utils.TimersIds;

public class SampleTimer extends ProtoTimer {

    public static final short TIMER_ID = TimersIds.SAMPLE_TIMER.getId();

    public SampleTimer() {
        super(TIMER_ID);
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }
}
