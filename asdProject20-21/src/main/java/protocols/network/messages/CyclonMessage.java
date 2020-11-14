package protocols.network.messages;

import babel.generic.ProtoMessage;
import io.netty.buffer.ByteBuf;
import network.ISerializer;
import network.data.Host;
import utils.MessageIds;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CyclonMessage extends ProtoMessage {

    public final static short MSG_ID = MessageIds.CYCLON_MESSAGE.getId();

    private final Map<Host, Integer> sample;

    public CyclonMessage(Map<Host, Integer> sample) {
        super(MSG_ID);
        this.sample = sample;
    }

    public Map<Host, Integer> getSample() {
        return sample;
    }

    @Override
    public String toString() {
        return "ShuffleMessage{" +
                "subset=" + sample +
                '}';
    }

    public static ISerializer<CyclonMessage> serializer = new ISerializer<CyclonMessage>() {
        @Override
        public void serialize(CyclonMessage sampleMessage, ByteBuf out) throws IOException {
            out.writeInt(sampleMessage.sample.size());
            for (Host h : sampleMessage.sample.keySet())
                Host.serializer.serialize(h, out);
        }

        @Override
        public CyclonMessage deserialize(ByteBuf in) throws IOException {
            int size = in.readInt();
            Map<Host, Integer> subset = new HashMap<>(size, 1);
            for (int i = 0; i < size; i++)
                subset.put(Host.serializer.deserialize(in), 0); // NAO DEVE SER 0 ?!!??!
            return new CyclonMessage(subset);
        }
    };
}
