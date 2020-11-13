package protocols.broadcast.eagerPushGossip;

import babel.core.GenericProtocol;
import babel.exceptions.HandlerRegistrationException;
import babel.generic.ProtoMessage;
import network.data.Host;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import protocols.broadcast.common.BroadcastRequest;
import protocols.broadcast.common.DeliverNotification;
import protocols.membership.common.notifications.ChannelCreated;
import protocols.membership.common.notifications.NeighbourDown;
import protocols.membership.common.notifications.NeighbourUp;
import protocols.broadcast.eagerPushGossip.messages.EPGMessage;

import java.io.IOException;
import java.util.*;

public class EagerPushGossip extends GenericProtocol {
	private static final Logger logger = LogManager.getLogger(EagerPushGossip.class);

	// Protocol information, to register in babel
	public static final String PROTOCOL_NAME = "EagerPushGossip";
	public static final short PROTOCOL_ID = 201;

	private final Host myself; // My own address/port
	private final Set<Host> neighbours; // My known neighbours (a.k.a peers the membership protocol told me about)
	private final Set<UUID> received; // Set of received messages (since we do not want to deliver the same msg twice)

	// We can only start sending messages after the membership protocol informed us
	// that the channel is ready
	private boolean channelReady;

	public EagerPushGossip(Properties properties, Host myself) throws IOException, HandlerRegistrationException {
		super(PROTOCOL_NAME, PROTOCOL_ID);
		this.myself = myself;
		neighbours = new HashSet<>();
		received = new HashSet<>();
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
		// Nothing to do here, we just wait for event from the membership or the
		// application
	}

	// Upon receiving the channelId from the membership, register our own callbacks
	// and serializers
	private void uponChannelCreated(ChannelCreated notification, short sourceProto) {
		int cId = notification.getChannelId();
		// Allows this protocol to receive events from this channel.
		registerSharedChannel(cId);
		/*---------------------- Register Message Serializers ---------------------- */
		registerMessageSerializer(cId, EPGMessage.MSG_ID, EPGMessage.serializer);
		/*---------------------- Register Message Handlers -------------------------- */
		try {
			registerMessageHandler(cId, EPGMessage.MSG_ID, this::uponGossipMessage, this::uponMsgFail);
		} catch (HandlerRegistrationException e) {
			logger.error("Error registering message handler: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
		// Now we can start sending messages
		channelReady = true;
	}

	/*--------------------------------- Requests ---------------------------------------- */
	private void uponBroadcastRequest(BroadcastRequest request, short sourceProto) {
		if (!channelReady)
			return;

		// Create the message object.
		EPGMessage msg = new EPGMessage(request.getMsgId(), request.getSender(), sourceProto, request.getMsg());

		// Call the same handler as when receiving a new GossipMessage (since the logic
		// is the same)
		uponGossipMessage(msg, myself, getProtoId(), -1);
	}

	/*--------------------------------- Messages ---------------------------------------- */
	private void uponGossipMessage(EPGMessage msg, Host from, short sourceProto, int channelId) {
		logger.trace("Received {} from {}", msg, from);
		// If we already received it once, do nothing (or we would end up with a nasty
		// infinite loop)
		if (received.add(msg.getMid())) {
			// Deliver the message to the application (even if it came from it)
			triggerNotification(new DeliverNotification(msg.getMid(), msg.getSender(), msg.getContent()));

			// Generate a random number using nextInt
			Random random = new Random();

			// Calculate fanout
			int fanout = (int) Math.ceil(Math.log(neighbours.size() + 1)); 
			
			logger.info("neighbours size = " + neighbours.size());
			
			int randomNumber;
			Set<Integer> randomValues = new HashSet<>();

			// PODEMOS GUARDAR OS HOSTS E VERIFICAR QUE NÃO ENVIA 2X PARA O MESMO PEER, 6
			// linhas abaixo
			int i = 0;
			while(i < fanout){
				// This will generate a random number between 0 and Set.size - 1
				randomNumber = random.nextInt(neighbours.size());
				if (randomValues.add(randomNumber)) {
					Host host = (Host) neighbours.toArray()[randomNumber];
					i++;
					// Send the message to random subset of neighbors
					if (!host.equals(from)) {
						logger.trace("Sent {} to {}", msg, host);
						sendMessage(msg, host);
					}
				}

			}

		}
	}

	private void uponMsgFail(ProtoMessage msg, Host host, short destProto, Throwable throwable, int channelId) {
		// If a message fails to be sent, for whatever reason, log the message and the
		// reason
		logger.error("Message {} to {} failed, reason: {}", msg, host, throwable);
	}

	private void uponNeighbourUp(NeighbourUp notification, short sourceProto) {
		for (Host h : notification.getNeighbours()) {
			neighbours.add(h);
			logger.info("New neighbour: " + h);
		}
	}

	private void uponNeighbourDown(NeighbourDown notification, short sourceProto) {
		for (Host h : notification.getNeighbours()) {
			neighbours.remove(h);
			logger.info("Neighbour down: " + h);
		}
	}
}
