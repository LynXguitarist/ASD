package protocols.apps.timers;

import babel.generic.ProtoTimer;
import utils.TimersIds;

public class StopTimer extends ProtoTimer {
    public static final short TIMER_ID = TimersIds.STOP_TIMER.getId();

    public StopTimer() {
        super(TIMER_ID);
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }
}
