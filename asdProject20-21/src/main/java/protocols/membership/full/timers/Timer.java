package protocols.membership.full.timers;

import babel.generic.ProtoTimer;


public class Timer extends ProtoTimer {


    public Timer(short timerID) {
        super(timerID);
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }



}
