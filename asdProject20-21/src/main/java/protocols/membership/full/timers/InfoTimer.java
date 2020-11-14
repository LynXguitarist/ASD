package protocols.membership.full.timers;

import babel.generic.ProtoTimer;
import utils.TimersIds;

public class InfoTimer extends ProtoTimer {

    public static final short TIMER_ID = TimersIds.INFO_TIMER.getId();

    public InfoTimer() {
        super(TIMER_ID);
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }
}
