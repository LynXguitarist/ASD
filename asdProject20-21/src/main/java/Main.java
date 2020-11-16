import babel.core.Babel;
import babel.core.GenericProtocol;
import babel.exceptions.HandlerRegistrationException;
import babel.exceptions.ProtocolAlreadyExistsException;
import network.data.Host;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import protocols.apps.BroadcastApp;
import protocols.broadcast.flood.FloodBroadcast;
import protocols.broadcast.eagerPushGossip.EagerPushGossip;
import protocols.broadcast.plumtree.PlumTree;
import protocols.membership.full.SimpleFullMembership;
import protocols.network.cyclon.Cyclon;
import utils.InterfaceToIp;
import utils.ProtocolsIds;
import utils.ProtocolsName;
import utils.Stats;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Properties;

public class Main {

	// Sets the log4j (logging library) configuration file
	static {
		System.setProperty("log4j.configurationFile", "log4j2.xml");
	}

	// Creates the logger object
	private static final Logger logger = LogManager.getLogger(Main.class);

	// Default babel configuration file (can be overridden by the "-config" launch
	// argument)
	private static final String DEFAULT_CONF = "babel_config.properties";

	private static Babel babel;
	
	public static void main(String[] args) throws Exception {

		// Get the (singleton) babel instance
		babel = Babel.getInstance();

		// Loads properties from the configuration file, and merges them with properties
		// passed in the launch arguments
		Properties props = Babel.loadConfig(args, DEFAULT_CONF);

		// If you pass an interface name in the properties (either file or arguments),
		// this wil get the IP of that interface
		// and create a property "address=ip" to be used later by the channels.
		InterfaceToIp.addInterfaceIp(props);

		// The Host object is an address/port pair that represents a network host. It is
		// used extensively in babel
		// It implements equals and hashCode, and also includes a serializer that makes
		// it easy to use in network messages
		Host myself = new Host(InetAddress.getByName(props.getProperty("address")),
				Integer.parseInt(props.getProperty("port")));

		logger.info("Hello, I am {}", myself);

		// Broadcast Protocol
		pickBroadcastProtocol(props.getProperty("broadcast_protocol"), props, myself);

		// Start babel and protocol threads
		babel.start();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> logger.info("Goodbye")));
	}

	// Init the protocols. This should be done after creating all protocols, since
	// there can be inter-protocol
	// communications in this step.
	private static void pickBroadcastProtocol(String config, Properties props, Host myself)
			throws ProtocolAlreadyExistsException, HandlerRegistrationException, IOException {
		GenericProtocol broadcast = null;
		GenericProtocol broadcastApp = null;
		try {
			if (config.toUpperCase().equals(ProtocolsName.EARGER_PUSH_GOSSIP.getName())) { // EagerPushGossip
				broadcast = new EagerPushGossip(props, myself);
				broadcastApp = new BroadcastApp(myself, props, ProtocolsIds.EARGER_PUSH_GOSSIP.getId());
				logger.info("Started broadcast EagerPushGossip...");
			} else if (config.toUpperCase().equals(ProtocolsName.PLUMTREE.getName())) { // PlumTree
				broadcast = new PlumTree(props, myself);
				broadcastApp = new BroadcastApp(myself, props, ProtocolsIds.PLUMTREE.getId());
				logger.info("Started broadcast PlumTree...");
			} else if (config.toUpperCase().equals(ProtocolsName.FLOOD.getName())) { // Flood
				broadcast = new FloodBroadcast(props, myself);
				broadcastApp = new BroadcastApp(myself, props, ProtocolsIds.FLOOD.getId());
				logger.info("Started broadcast Flood...");
			} else
				throw new NullPointerException("Invalid Broadcast Protocol!");
		} catch (IOException | HandlerRegistrationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		GenericProtocol membership = pickMembershipProtocol(props.getProperty("membership_protocol"), props, myself);
		
		// Register applications in babel
		babel.registerProtocol(broadcastApp);
		babel.registerProtocol(broadcast);
		babel.registerProtocol(membership);

		broadcastApp.init(props);
		broadcast.init(props);
		membership.init(props);
	}

	// Init the protocols. This should be done after creating all protocols, since
	// there can be inter-protocol
	// communications in this step.
	private static GenericProtocol pickMembershipProtocol(String config, Properties props, Host myself) {
		GenericProtocol membership = null;
		try {
			if (config.toUpperCase().equals(ProtocolsName.CYCLON.getName())) { // Cyclon
				membership = new Cyclon(props, myself);
				logger.info("Started membership Cyclon...");
				return membership;
			} else if (config.toUpperCase().equals(ProtocolsName.SIMPLE_FULL_MEMBERSHIP.getName())) { // SimpleFullMembership
				membership = new SimpleFullMembership(props, myself);
				logger.info("Started membership SimpleFullMemberShip...");
				return membership;
			} else
				throw new NullPointerException("Invalid Membership Protocol!");
		} catch (IOException | HandlerRegistrationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}
