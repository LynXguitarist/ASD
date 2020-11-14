package protocols.apps.timers;

import babel.generic.ProtoTimer;
import utils.TimersIds;

public class StartTimer extends ProtoTimer {
    public static final short TIMER_ID = TimersIds.START_TIMER.getId();

    public StartTimer() {
        super(TIMER_ID);
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }
}
