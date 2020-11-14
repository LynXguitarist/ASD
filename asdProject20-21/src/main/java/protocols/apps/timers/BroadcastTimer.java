package protocols.apps.timers;

import babel.generic.ProtoTimer;
import utils.TimersIds;

public class BroadcastTimer extends ProtoTimer {
    public static final short TIMER_ID = TimersIds.BROADCAST_TIMER.getId();

    public BroadcastTimer() {
        super(TIMER_ID);
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }
}
