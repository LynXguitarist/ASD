package protocols.network.cyclon.notifications;

import java.util.HashSet;
import java.util.Set;

import babel.generic.ProtoNotification;
import network.data.Host;

public class Neighbours extends ProtoNotification {

    public static final short NOTIFICATION_ID = 102;

    private final Set<Host> neighbours;

    public Neighbours(Set<Host>neighbours) {
        super(NOTIFICATION_ID);
        this.neighbours = neighbours;
    }
    
    public Set<Host> getNeighbours() {
        return new HashSet<>(this.neighbours);
    }
    
    public int getLength() {
    	return this.neighbours.size();
    }
}