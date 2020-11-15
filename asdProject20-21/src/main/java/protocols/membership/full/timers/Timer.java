package protocols.membership.full.timers;

import babel.generic.ProtoTimer;
import utils.MessageIds;


public class Timer extends ProtoTimer {

    public static final short TIMER_ID = 2111;
    private final short timerID;


    public Timer(short timerID) {
        super(TIMER_ID);
        this.timerID = timerID;
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }


    public short getTimerId(){
        return this.timerID;
    }



}
