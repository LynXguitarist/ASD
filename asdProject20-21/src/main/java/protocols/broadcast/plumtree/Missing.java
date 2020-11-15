package protocols.broadcast.plumtree;

import network.data.Host;

import java.util.UUID;

public class Missing {

    private final UUID mid;
    private final Host sender;
    private final long id;

    public Missing( UUID mid,Host sender, long id){
        this.mid = mid;
        this.sender = sender;
        this.id = id;
    }

    public Host getSender() {
        return sender;
    }

    public UUID getMid(){
        return mid;
    }

    public long getId(){
        return id;
    }


}
