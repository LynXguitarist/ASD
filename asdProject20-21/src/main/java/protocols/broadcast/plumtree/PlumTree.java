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
import protocols.broadcast.plumtree.messages.GraftMessage;
import protocols.broadcast.plumtree.messages.IHaveMessage;
import protocols.broadcast.plumtree.messages.PruneMessage;
import protocols.membership.common.notifications.ChannelCreated;
import protocols.membership.common.notifications.NeighbourDown;
import protocols.membership.common.notifications.NeighbourUp;
import protocols.membership.full.timers.Timer;
import utils.ProtocolsIds;

import java.io.IOException;
import java.util.*;


public class PlumTree extends GenericProtocol {
    private static final Logger logger = LogManager.getLogger(PlumTree.class);

    //Protocol information, to register in babel
    public static final String PROTOCOL_NAME = "PlumTree";
    public static final short PROTOCOL_ID = ProtocolsIds.PLUMTREE.getId();;

    private final Host myself; //My own address/port
    private final Set<Host> neighbours; //My known neighbours (a.k.a peers the membership protocol told me about)
    private final Map<UUID,byte[] > received; //Set of received messages (since we do not want to deliver the same msg twice)

    private final Set<Host> eagerPushPeers;
    private final Set<Host> lazyPushPeers;

    private final Set<IHaveMessage> lazyQueue;
    private final Set<Missing> missing;

    private final Set<Timer> timers;

    private final int lostMessageTimeOut; //param: timeout for samples

    //We can only start sending messages after the membership protocol informed us that the channel is ready
    private boolean channelReady;

    public PlumTree(Properties properties, Host myself) throws IOException, HandlerRegistrationException {
        super(PROTOCOL_NAME, PROTOCOL_ID);
        this.myself = myself;
        neighbours = new HashSet<>();
        received = new HashMap<>();

        eagerPushPeers = new HashSet<>();
        lazyPushPeers = new HashSet<>();

        lazyQueue = new HashSet<>();
        missing = new HashSet<>();

        this.timers = new HashSet<>();
        this.lostMessageTimeOut = Integer.parseInt(properties.getProperty("lostMessageTimeOut", "2000")); //2 seconds

        channelReady = false;

        /*--------------------- Register Request Handlers ----------------------------- */
        registerRequestHandler(BroadcastRequest.REQUEST_ID, this::uponBroadcastRequest);

        /*--------------------- Register Notification Handlers ----------------------------- */
        subscribeNotification(NeighbourUp.NOTIFICATION_ID, this::uponNeighbourUp);
        subscribeNotification(NeighbourDown.NOTIFICATION_ID, this::uponNeighbourDown);
        subscribeNotification(ChannelCreated.NOTIFICATION_ID, this::uponChannelCreated);

        timers.forEach(timer -> {
            try {
                registerTimerHandler(timer.getId(), this::uponTimer);
            } catch (HandlerRegistrationException e) {
                e.printStackTrace();
            }
        });
    }

    private void uponTimer(Timer timer, long timerId) {
        setupTimer(timer, lostMessageTimeOut );
        timers.add(timer);
        missing.forEach(missingElem->{
            if(missingElem.getId()==timer.getId()){
               eagerPushPeers.add(missingElem.getSender());
               lazyPushPeers.remove(missingElem.getSender());
               sendMessage(new GraftMessage(missingElem.getMid(),myself), missingElem.getSender());
            }
        });
    }

    @Override
    public void init(Properties props) {
    }

    //Upon receiving the channelId from the membership, register our own callbacks and serializers
    private void uponChannelCreated(ChannelCreated notification, short sourceProto) {
        int cId = notification.getChannelId();
        // Allows this protocol to receive events from this channel.
        registerSharedChannel(cId);
        /*---------------------- Register Message Serializers ---------------------- */
        registerMessageSerializer(cId, FloodMessage.MSG_ID, FloodMessage.serializer);
        registerMessageSerializer(cId, PruneMessage.MSG_ID, PruneMessage.serializer);
        registerMessageSerializer(cId, IHaveMessage.MSG_ID, IHaveMessage.serializer);
        registerMessageSerializer(cId, GraftMessage.MSG_ID, GraftMessage.serializer);

        /*---------------------- Register Message Handlers -------------------------- */

        //Handler for FloodMessage
        try {
            registerMessageHandler(cId, FloodMessage.MSG_ID, this::uponBroadcastMessage, this::uponMsgFail);
        } catch (HandlerRegistrationException e) {
            logger.error("Error registering message handler: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        //Handler for PruneMessage
        try {
            registerMessageHandler(cId, PruneMessage.MSG_ID, this::uponPruneMessage, this::uponMsgFail);
        } catch (HandlerRegistrationException e) {
            logger.error("Error registering message handler: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        //Handler for IHaveMessage
        try {
            registerMessageHandler(cId, IHaveMessage.MSG_ID, this::uponIHaveMessage, this::uponMsgFail);
        } catch (HandlerRegistrationException e) {
            logger.error("Error registering message handler: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        //Handler for GraftMessage
        try {
            registerMessageHandler(cId, GraftMessage.MSG_ID, this::uponGraftMessage, this::uponMsgFail);
        } catch (HandlerRegistrationException e) {
            logger.error("Error registering message handler: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        //Now we can start sending messages
        channelReady = true;
    }

    private void uponGraftMessage(GraftMessage graftMessage, Host from, short sourceProto, int channelId) {
        eagerPushPeers.add(graftMessage.getSender());
        lazyPushPeers.remove(graftMessage.getSender());

        if(received.containsKey(graftMessage.getMid())){

            logger.info("FLOODMESSAGE__GRAFT");
            sendMessage(new FloodMessage(graftMessage.getMid(), myself, sourceProto, received.get(graftMessage.getMid())), graftMessage.getSender());

        }

    }

    private void uponIHaveMessage(IHaveMessage iHaveMessage, Host from, short sourceProto, int channelId) {

        if (!received.containsKey(iHaveMessage.getMid())){
            if(!timers.contains(new Timer(iHaveMessage.getId()))){
                Timer t = new Timer(iHaveMessage.getId());
                setupTimer(t, lostMessageTimeOut );
                timers.add(t);
            }

            missing.add(new Missing(iHaveMessage.getMid(),iHaveMessage.getSender(), iHaveMessage.getId()));
        }
    }

    private void uponPruneMessage(PruneMessage msg, Host from, short sourceProto, int channelId) {
        eagerPushPeers.remove(msg.getSender());
        lazyPushPeers.add(msg.getSender());
    }

    /*--------------------------------- Requests ---------------------------------------- */
    private void uponBroadcastRequest(BroadcastRequest request, short sourceProto) {
        if (!channelReady) return;

        //Create the message object.
        logger.info("FLOODMESSAGE__BROADCAST");
        FloodMessage msg = new FloodMessage(request.getMsgId(), request.getSender(), sourceProto, request.getMsg());

        //Call the same handler as when receiving a new FloodMessage (since the logic is the same)
        uponBroadcastMessage(msg, myself, getProtoId(), -1);
    }

    /*--------------------------------- Messages ---------------------------------------- */
    private void uponBroadcastMessage(FloodMessage msg, Host from, short sourceProto, int channelId) {
        logger.trace("Received {} from {}", msg, from);

        logger.info("*****MID****** :"+  msg.getMid());
        logger.info("*****FROM****** :"+  from);

        //If we already received it once, do nothing (or we would end up with a nasty infinite loop)
        if (!received.containsKey(msg.getMid())) {
            logger.info("******PRIMEIRA_VEZ******");
            received.put(msg.getMid(),msg.getContent());
            //Deliver the message to the application (even if it came from it)
            triggerNotification(new DeliverNotification(msg.getMid(), msg.getSender(), msg.getContent()));


           if( missing.contains(new Missing(msg.getMid(), msg.getSender(), msg.getId()))){
               timers.forEach(timer -> {
                   if(timer.getId() == msg.getId()){
                       cancelTimer(msg.getId());
                   }
               });
           }

            eagerPush(msg, myself);
            lazyPush(msg, myself);

            eagerPushPeers.add(msg.getSender());
            lazyPushPeers.remove(msg.getSender());
        } else {
            logger.info("******SEGUNDA_VEZ******");
            eagerPushPeers.remove(msg.getSender());
            lazyPushPeers.add(msg.getSender());

            if(!myself.equals(msg.getSender())){
                sendMessage(new PruneMessage(UUID.randomUUID(),myself),msg.getSender());
            }

        }
    }

    private void eagerPush(FloodMessage msg, Host myself){
        eagerPushPeers.forEach(host->{
            if (!host.equals(myself)) {
                logger.trace("Sent {} to {}", msg, host);
                logger.info("*****EAGER_PUSH****** :");
                sendMessage(new FloodMessage(msg.getMid(), myself, msg.getToDeliver(), msg.getContent()), host);
            }
        });
    }

    private void lazyPush(FloodMessage msg, Host myself){
        lazyPushPeers.forEach(host->{
            if(!host.equals(myself)) {
                lazyQueue.add(new IHaveMessage(msg.getMid(), host, msg.getContent()));
            }
        });
        dispatch();
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
            eagerPushPeers.add(h);
        }
    }

    private void uponNeighbourDown(NeighbourDown notification, short sourceProto) {
        for(Host h: notification.getNeighbours()) {
            neighbours.remove(h);
            logger.info("Neighbour down: " + h);

            eagerPushPeers.remove(h);
            lazyPushPeers.remove(h);

            missing.forEach(missingMessage->{
                if(missingMessage.getSender().equals(h)){
                    missing.remove(missingMessage);
                }
            });
        }
    }

    private void dispatch(){
        lazyQueue.forEach(message->{
            sendMessage(message,message.getSender());
        });
        lazyQueue.clear();
    }


}
