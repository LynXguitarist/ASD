package protocols.apps.timers;

import babel.generic.ProtoTimer;
import utils.TimersIds;

public class ExitTimer extends ProtoTimer {
    public static final short TIMER_ID = TimersIds.EXIT_TIMER.getId();

    public ExitTimer() {
        super(TIMER_ID);
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }
}
