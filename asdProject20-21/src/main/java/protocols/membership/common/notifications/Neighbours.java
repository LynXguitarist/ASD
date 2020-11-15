package protocols.membership.common.notifications;

import java.util.HashSet;
import java.util.Set;

import babel.generic.ProtoNotification;
import network.data.Host;

public class Neighbours extends ProtoNotification {

    public static final short NOTIFICATION_ID = 104;

    private final Set<Host> neighbours;

    public Neighbours(Set<Host> neighbour) {
        super(NOTIFICATION_ID);
        this.neighbours = neighbour;
    }

    public Set<Host> getNeighbours() {
        return new HashSet<>(this.neighbours);
    }
    
    public int getLength() {
    	return this.neighbours.size();
    }
}
