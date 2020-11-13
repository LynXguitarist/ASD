package protocols.broadcast.plumtree;

import babel.core.GenericProtocol;
import babel.exceptions.HandlerRegistrationException;
import babel.generic.ProtoMessage;
import network.data.Host;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import protocols.broadcast.common.BroadcastRequest;
import protocols.broadcast.common.DeliverNotification;
import protocols.broadcast.flood.messages.FloodMessage;
import protocols.membership.common.notifications.ChannelCreated;
import protocols.membership.common.notifications.NeighbourDown;
import protocols.membership.common.notifications.NeighbourUp;

import java.io.IOException;
import java.util.*;


public class PlumTree extends GenericProtocol {
    private static final Logger logger = LogManager.getLogger(PlumTree.class);

    //Protocol information, to register in babel
    public static final String PROTOCOL_NAME = "PlumTree";
    public static final short PROTOCOL_ID = 202;

    private final Host myself; //My own address/port
    private final Set<Host> neighbours; //My known neighbours (a.k.a peers the membership protocol told me about)
    private final Set<UUID> received; //Set of received messages (since we do not want to deliver the same msg twice)

    private final Set<Host> eagerPushPeers;
    private final Set<Host> lazyPushPeers;
    //IHAVEMESSAGES
    //private final lazyQueue;


    //We can only start sending messages after the membership protocol informed us that the channel is ready
    private boolean channelReady;

    public PlumTree(Properties properties, Host myself) throws IOException, HandlerRegistrationException {
        super(PROTOCOL_NAME, PROTOCOL_ID);
        this.myself = myself;
        neighbours = new HashSet<>();
        received = new HashSet<>();

        eagerPushPeers = new HashSet<>();
        lazyPushPeers = new HashSet<>();

        channelReady = false;

        /*--------------------- Register Request Handlers ----------------------------- */
        registerRequestHandler(BroadcastRequest.REQUEST_ID, this::uponBroadcastRequest);

        /*--------------------- Register Notification Handlers ----------------------------- */
        subscribeNotification(NeighbourUp.NOTIFICATION_ID, this::uponNeighbourUp);
        subscribeNotification(NeighbourDown.NOTIFICATION_ID, this::uponNeighbourDown);
        subscribeNotification(ChannelCreated.NOTIFICATION_ID, this::uponChannelCreated);
    }

    @Override
    public void init(Properties props) {

        Random random = new Random();

        int fanout = (int) Math.ceil(Math.log(neighbours.size() + 1));

        int randomNumber;
        Set<Integer> randomValues = new HashSet<>();
        logger.info("neighbors :" +  neighbours.size());
        int i = 0;
        while(i < fanout){
            // This will generate a random number between 0 and Set.size - 1
            randomNumber = random.nextInt(neighbours.size());
            if (randomValues.add(randomNumber)) {
                Host host = (Host) neighbours.toArray()[randomNumber];
                eagerPushPeers.add(host);
                i++;
            }
        }
    }

    //Upon receiving the channelId from the membership, register our own callbacks and serializers
    private void uponChannelCreated(ChannelCreated notification, short sourceProto) {
        int cId = notification.getChannelId();
        // Allows this protocol to receive events from this channel.
        registerSharedChannel(cId);
        /*---------------------- Register Message Serializers ---------------------- */
        registerMessageSerializer(cId, FloodMessage.MSG_ID, FloodMessage.serializer);
        /*---------------------- Register Message Handlers -------------------------- */
        try {
            registerMessageHandler(cId, FloodMessage.MSG_ID, this::uponBroadcastMessage, this::uponMsgFail);
        } catch (HandlerRegistrationException e) {
            logger.error("Error registering message handler: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        //Now we can start sending messages
        channelReady = true;
    }

    /*--------------------------------- Requests ---------------------------------------- */
    private void uponBroadcastRequest(BroadcastRequest request, short sourceProto) {
        if (!channelReady) return;

        //Create the message object.
        FloodMessage msg = new FloodMessage(request.getMsgId(), request.getSender(), sourceProto, request.getMsg());

        //Call the same handler as when receiving a new FloodMessage (since the logic is the same)
        uponBroadcastMessage(msg, myself, getProtoId(), -1);
    }

    /*--------------------------------- Messages ---------------------------------------- */
    private void uponBroadcastMessage(FloodMessage msg, Host from, short sourceProto, int channelId) {
        logger.trace("Received {} from {}", msg, from);

        logger.info("Message Id: " + msg.getMid());

        //If we already received it once, do nothing (or we would end up with a nasty infinite loop)
        if (received.add(msg.getMid())) {
            //Deliver the message to the application (even if it came from it)
            triggerNotification(new DeliverNotification(msg.getMid(), msg.getSender(), msg.getContent()));

            eagerPush(msg, myself);
            lazyPush(msg, myself);

            eagerPushPeers.add(from);
            lazyPushPeers.remove(from);
        } else {
            eagerPushPeers.remove(from);
            lazyPushPeers.add(from);
        }
    }

    private void eagerPush(FloodMessage msg, Host myself){
        eagerPushPeers.forEach(host->{
            if (!host.equals(myself)) {
                logger.trace("Sent {} to {}", msg, host);
                sendMessage(msg, host);
            }
        });
    }

    private void lazyPush(FloodMessage msg, Host myself){
        lazyPushPeers.forEach(host->{
            //lazyQueue.add()
        });
    }

    private void uponMsgFail(ProtoMessage msg, Host host, short destProto,
                             Throwable throwable, int channelId) {
        //If a message fails to be sent, for whatever reason, log the message and the reason
        logger.error("Message {} to {} failed, reason: {}", msg, host, throwable);
    }

    /*--------------------------------- Notifications ---------------------------------------- */

    //When the membership protocol notifies of a new neighbour (or leaving one) simply update my list of neighbours.
    private void uponNeighbourUp(NeighbourUp notification, short sourceProto) {
        for(Host h: notification.getNeighbours()) {
            neighbours.add(h);
            logger.info("New neighbour: " + h);
        }
    }

    private void uponNeighbourDown(NeighbourDown notification, short sourceProto) {
        for(Host h: notification.getNeighbours()) {
            neighbours.remove(h);
            logger.info("Neighbour down: " + h);
        }
    }
}
