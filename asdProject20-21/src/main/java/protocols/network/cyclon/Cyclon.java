package protocols.network.cyclon;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import babel.core.GenericProtocol;
import babel.exceptions.HandlerRegistrationException;
import babel.generic.ProtoMessage;
import channel.tcp.TCPChannel;
import channel.tcp.events.ChannelMetrics;
import channel.tcp.events.InConnectionDown;
import channel.tcp.events.InConnectionUp;
import channel.tcp.events.OutConnectionDown;
import channel.tcp.events.OutConnectionFailed;
import channel.tcp.events.OutConnectionUp;
import network.data.Host;
import protocols.membership.common.notifications.ChannelCreated;
import protocols.membership.common.notifications.NeighbourDown;
import protocols.membership.common.notifications.NeighbourUp;
import protocols.network.messages.CyclonMessage;
import protocols.network.messages.CyclonMessageMerge;
import protocols.network.timers.CyclonInfoTimer;
import protocols.network.timers.CyclonSampleTimer;
import utils.ProtocolsIds;

public class Cyclon extends GenericProtocol {

	private static final Logger logger = LogManager.getLogger(Cyclon.class);
	private static final int CACHE_SIZE = 50; // cache size -> fixed-sized cache of c entries

	// Protocol information, to register in babel
	public final static short PROTOCOL_ID = ProtocolsIds.CYCLON.getId();
	public final static String PROTOCOL_NAME = "Cyclon";

	private final Host self; // My own address/port
	private final Map<Host, Integer> membership; // Peers I am connected to
	private final Map<Host, Integer> pending; // Peers I am trying to connect to

	private final int sampleTime; // param: timeout for samples
	private final int subsetSize; // param: maximum size of sample;

	// private Map<Host, Integer> pview; // Neighbors sent in the last shuffle
	private Map<Host, Integer> sampleHosts;

	private final Random rnd;

	private final int channelId; // Id of the created channel

	public Cyclon(Properties props, Host self) throws IOException, HandlerRegistrationException {
		super(PROTOCOL_NAME, PROTOCOL_ID);

		this.self = self;
		this.membership = new HashMap<>();
		this.pending = new HashMap<>();
		// this.pview = new HashMap<>();

		this.sampleHosts = new HashMap<>();
		this.rnd = new Random();

		// Get some configurations from the Properties object
		this.subsetSize = Integer.parseInt(props.getProperty("sample_size", "6"));
		this.sampleTime = Integer.parseInt(props.getProperty("sample_time", "2000")); // 2 seconds

		String cMetricsInterval = props.getProperty("channel_metrics_interval", "10000"); // 10 seconds

		// Create a properties object to setup channel-specific properties. See the
		// channel description for more details.
		Properties channelProps = new Properties();
		channelProps.setProperty(TCPChannel.ADDRESS_KEY, props.getProperty("address")); // The address to bind to
		channelProps.setProperty(TCPChannel.PORT_KEY, props.getProperty("port")); // The port to bind to
		channelProps.setProperty(TCPChannel.METRICS_INTERVAL_KEY, cMetricsInterval); // The interval to receive channel
																						// metrics
		channelProps.setProperty(TCPChannel.HEARTBEAT_INTERVAL_KEY, "1000"); // Heartbeats interval for established
																				// connections
		channelProps.setProperty(TCPChannel.HEARTBEAT_TOLERANCE_KEY, "3000"); // Time passed without heartbeats until
																				// closing a connection
		channelProps.setProperty(TCPChannel.CONNECT_TIMEOUT_KEY, "1000"); // TCP connect timeout
		channelId = createChannel(TCPChannel.NAME, channelProps); // Create the channel with the given properties

		/*---------------------- Register Message Serializers ---------------------- */
		registerMessageSerializer(channelId, CyclonMessage.MSG_ID, CyclonMessage.serializer);
		registerMessageSerializer(channelId, CyclonMessageMerge.MSG_ID, CyclonMessageMerge.serializer);

		/*---------------------- Register Message Handlers -------------------------- */
		registerMessageHandler(channelId, CyclonMessage.MSG_ID, this::uponReceiveShuffle, this::uponMsgFail);
		registerMessageHandler(channelId, CyclonMessageMerge.MSG_ID, this::uponReceiveShuffleReply, this::uponMsgFail);

		/*--------------------- Register Timer Handlers ----------------------------- */
		registerTimerHandler(CyclonSampleTimer.TIMER_ID, this::uponShuffle);
		registerTimerHandler(CyclonInfoTimer.TIMER_ID, this::uponInfoTime);

		/*-------------------- Register Channel Events ------------------------------- */

		registerChannelEventHandler(channelId, OutConnectionDown.EVENT_ID, this::uponOutConnectionDown);
		registerChannelEventHandler(channelId, OutConnectionFailed.EVENT_ID, this::uponOutConnectionFailed);
		registerChannelEventHandler(channelId, OutConnectionUp.EVENT_ID, this::uponOutConnectionUp);
		registerChannelEventHandler(channelId, InConnectionUp.EVENT_ID, this::uponInConnectionUp);
		registerChannelEventHandler(channelId, InConnectionDown.EVENT_ID, this::uponInConnectionDown);
		registerChannelEventHandler(channelId, ChannelMetrics.EVENT_ID, this::uponChannelMetrics);
	}

	@Override
	public void init(Properties props) {
		// Inform the dissemination protocol about the channel we created in the
		// constructor
		triggerNotification(new ChannelCreated(channelId));

		// If there is a contact node, attempt to establish connection
		if (props.containsKey("contact")) {
			try {
				String contact = props.getProperty("contact");
				String[] hostElems = contact.split(":");
				Host contactHost = new Host(InetAddress.getByName(hostElems[0]), Short.parseShort(hostElems[1]));
				// We add to the pending set until the connection is successful
				pending.put(contactHost, 0);
				openConnection(contactHost);
				membership.put(contactHost, 0);
			} catch (Exception e) {
				logger.error("Invalid contact on configuration: '" + props.getProperty("contacts"));
				e.printStackTrace();
				System.exit(-1);
			}
		}

		// Setup the timer used to send samples (we registered its handler on the
		// constructor)
		setupPeriodicTimer(new CyclonSampleTimer(), this.sampleTime, this.sampleTime);

		// Setup the timer to display protocol information (also registered handler
		// previously)
		int pMetricsInterval = Integer.parseInt(props.getProperty("protocol_metrics_interval", "10000"));
		if (pMetricsInterval > 0)
			setupPeriodicTimer(new CyclonInfoTimer(), pMetricsInterval, pMetricsInterval);
	}

	private void uponShuffle(CyclonSampleTimer timer, long timerId) {
		logger.info("started shuffling...");
		Entry<Host, Integer> oldest = null; // oldest neigh -> p in the algorithm
		for (Map.Entry<Host, Integer> entry : membership.entrySet()) {
			int newAge = entry.getValue() + 1;
			entry.setValue(newAge); // age + 1
			if (oldest == null || oldest.getValue() < newAge)
				oldest = entry;
		}

		if (oldest != null) {
			logger.info("Oldest Neighbour: " + oldest.getKey());
			sampleHosts = getRandomSubsetExcluding(membership, subsetSize, oldest.getKey());
			sampleHosts.put(self, 0);

			logger.info("Sample Hosts: " + sampleHosts);
			logger.info("Sending request to oldest...");
			sendMessage(new CyclonMessage(sampleHosts), oldest.getKey());
		}
	}

	private void uponReceiveShuffle(CyclonMessage msg, Host to, short sourceProto, int channelId) {
		logger.debug("Sending {} to {}", msg, to);
		// temporarySample
		Map<Host, Integer> tmpSample = getRandomSubset(membership, membership.size());

		Map<Host, Integer> samplePeers = msg.getSample();
		mergeView(samplePeers, tmpSample);

		// Trigger Send (ShuffleReply, s, temporarySample);
		sendMessage(new CyclonMessageMerge(tmpSample), to);
	}

	private void uponReceiveShuffleReply(CyclonMessageMerge msg, Host from, short sourceProto, int channelId) {
		logger.debug("Received {} from {}", msg, from);
		mergeView(msg.getSample(), sampleHosts);
	}

	private void mergeView(Map<Host, Integer> samplePeers, Map<Host, Integer> mySample) {
		logger.info("Merging view...");
		Map<Host, Integer> result = membership;

		for (Map.Entry<Host, Integer> entry : samplePeers.entrySet()) {
			Host peer = entry.getKey();
			int age = entry.getValue();
			if (membership.containsKey(peer) && membership.get(peer) > age) {
				logger.info("Updating peer: " + peer + " to age: " + age);
				membership.put(peer, age);
			} else if (membership.size() < CACHE_SIZE) {
				logger.info("Adding peer: " + peer + " with age: " + age + " to neigh...");
				membership.put(peer, age);
				openConnection(peer);
			} else {
				result.keySet().retainAll(mySample.keySet());
				// elem in neigh that is also in mySample
				Host h = null;
				logger.info("Host h:" + h);
				if (result.isEmpty()) {
					logger.info("There are no neigh...");
					// random elem to be replaced
					h = getRandom(membership.keySet());
				} else {
					logger.info("Get random peer from neigh...");
					h = getRandom(result.keySet());
				}
				logger.info("Closing connection with peer: " + h);
				membership.remove(h);
				closeConnection(h);

				logger.info("Opening connection with peer: " + peer);
				membership.put(peer, age);
				openConnection(peer);
			}
		}
	}

	/*--------------------------------- Messages ---------------------------------------- */

	private void uponMsgFail(ProtoMessage msg, Host host, short destProto, Throwable throwable, int channelId) {
		// If a message fails to be sent, for whatever reason, log the message and the
		// reason
		logger.error("Message {} to {} failed, reason: {}", msg, host, throwable);
	}

	/*--------------------------------- Timers ---------------------------------------- */

	// Gets a random element from the set of peers
	private Host getRandom(Set<Host> hostSet) {
		int idx = rnd.nextInt(hostSet.size());
		int i = 0;
		for (Host h : hostSet) {
			if (i == idx)
				return h;
			i++;
		}
		return null;
	}

	// Gets a random subset from the set of peers, excluding one
	private static Map<Host, Integer> getRandomSubsetExcluding(Map<Host, Integer> hostMap, int sampleSize,
			Host exclude) {
		List<Host> keys = new ArrayList<>(hostMap.keySet());
		keys.remove(exclude);
		Collections.shuffle(keys);

		Map<Host, Integer> shuffleMap = new LinkedHashMap<>(Math.min(sampleSize, keys.size()));
		keys.forEach(k -> shuffleMap.put(k, hostMap.get(k)));
		return shuffleMap;
	}

	// Gets a random subset from the set of peers
	private static Map<Host, Integer> getRandomSubset(Map<Host, Integer> hostMap, int sampleSize) {
		List<Host> keys = new ArrayList<>(hostMap.keySet());
		Collections.shuffle(keys);

		Map<Host, Integer> shuffleMap = new LinkedHashMap<>(Math.min(sampleSize, keys.size()));
		keys.forEach(k -> shuffleMap.put(k, hostMap.get(k)));
		return shuffleMap;
	}

	/*
	 * --------------------------------- TCPChannel Events
	 * ----------------------------
	 */

	// If a connection is successfully established, this event is triggered. In this
	// protocol, we want to add the
	// respective peer to the membership, and inform the Dissemination protocol via
	// a notification.
	private void uponOutConnectionUp(OutConnectionUp event, int channelId) {
		Host peer = event.getNode();
		logger.debug("Connection to {} is up", peer);
		pending.remove(peer);
		if (membership.put(peer, 0) == null) {
			triggerNotification(new NeighbourUp(peer));
		}
	}

	// If an established connection is disconnected, remove the peer from the
	// membership and inform the Dissemination
	// protocol. Alternatively, we could do smarter things like retrying the
	// connection X times.
	private void uponOutConnectionDown(OutConnectionDown event, int channelId) {
		Host peer = event.getNode();
		logger.debug("Connection to {} is down cause {}", peer, event.getCause());
		membership.remove(event.getNode());
		triggerNotification(new NeighbourDown(event.getNode()));
	}

	// If a connection fails to be established, this event is triggered. In this
	// protocol, we simply remove from the
	// pending set. Note that this event is only triggered while attempting a
	// connection, not after connection.
	// Thus the peer will be in the pending set, and not in the membership (unless
	// something is very wrong with our code)
	private void uponOutConnectionFailed(OutConnectionFailed<ProtoMessage> event, int channelId) {
		logger.debug("Connection to {} failed cause: {}", event.getNode(), event.getCause());
		pending.remove(event.getNode());
	}

	// If someone established a connection to me, this event is triggered. In this
	// protocol we do nothing with this event.
	// If we want to add the peer to the membership, we will establish our own
	// outgoing connection.
	// (not the smartest protocol, but its simple)
	private void uponInConnectionUp(InConnectionUp event, int channelId) {
		logger.trace("Connection from {} is up", event.getNode());
	}

	// A connection someone established to me is disconnected.
	private void uponInConnectionDown(InConnectionDown event, int channelId) {
		logger.trace("Connection from {} is down, cause: {}", event.getNode(), event.getCause());
	}

	/* --------------------------------- Metrics ---------------------------- */

	// If we setup the InfoTimer in the constructor, this event will be triggered
	// periodically.
	// We are simply printing some information to present during runtime.
	private void uponInfoTime(CyclonInfoTimer timer, long timerId) {
		StringBuilder sb = new StringBuilder("Membership Metrics:\n");
		sb.append("Membership: ").append(membership).append("\n");
		sb.append("PendingMembership: ").append(pending).append("\n");
		// getMetrics returns an object with the number of events of each type processed
		// by this protocol.
		// It may or may not be useful to you, but at least you know it exists.
		sb.append(getMetrics());
		logger.info(sb);
	}

	// If we passed a value >0 in the METRICS_INTERVAL_KEY property of the channel,
	// this event will be triggered
	// periodically by the channel. This is NOT a protocol timer, but a channel
	// event.
	// Again, we are just showing some of the information you can get from the
	// channel, and use how you see fit.
	// "getInConnections" and "getOutConnections" returns the currently established
	// connection to/from me.
	// "getOldInConnections" and "getOldOutConnections" returns connections that
	// have already been closed.
	private void uponChannelMetrics(ChannelMetrics event, int channelId) {
		StringBuilder sb = new StringBuilder("Channel Metrics:\n");
		sb.append("In channels:\n");
		event.getInConnections()
				.forEach(c -> sb.append(
						String.format("\t%s: msgOut=%s (%s) msgIn=%s (%s)\n", c.getPeer(), c.getSentAppMessages(),
								c.getSentAppBytes(), c.getReceivedAppMessages(), c.getReceivedAppBytes())));
		event.getOldInConnections()
				.forEach(c -> sb.append(
						String.format("\t%s: msgOut=%s (%s) msgIn=%s (%s) (old)\n", c.getPeer(), c.getSentAppMessages(),
								c.getSentAppBytes(), c.getReceivedAppMessages(), c.getReceivedAppBytes())));
		sb.append("Out channels:\n");
		event.getOutConnections()
				.forEach(c -> sb.append(
						String.format("\t%s: msgOut=%s (%s) msgIn=%s (%s)\n", c.getPeer(), c.getSentAppMessages(),
								c.getSentAppBytes(), c.getReceivedAppMessages(), c.getReceivedAppBytes())));
		event.getOldOutConnections()
				.forEach(c -> sb.append(
						String.format("\t%s: msgOut=%s (%s) msgIn=%s (%s) (old)\n", c.getPeer(), c.getSentAppMessages(),
								c.getSentAppBytes(), c.getReceivedAppMessages(), c.getReceivedAppBytes())));
		sb.setLength(sb.length() - 1);
		logger.info(sb);
	}

}
