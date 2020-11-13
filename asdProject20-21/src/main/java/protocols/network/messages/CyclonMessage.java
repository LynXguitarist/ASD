package protocols.network.messages;

import babel.generic.ProtoMessage;
import io.netty.buffer.ByteBuf;
import network.ISerializer;
import network.data.Host;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class CyclonMessage extends ProtoMessage {

    public final static short MSG_ID = 101;

    private final Set<Host> sample;

    public CyclonMessage(Set<Host> sample) {
        super(MSG_ID);
        this.sample = sample;
    }

    public Set<Host> getSample() {
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
            for (Host h : sampleMessage.sample)
                Host.serializer.serialize(h, out);
        }

        @Override
        public CyclonMessage deserialize(ByteBuf in) throws IOException {
            int size = in.readInt();
            Set<Host> subset = new HashSet<>(size, 1);
            for (int i = 0; i < size; i++)
                subset.add(Host.serializer.deserialize(in));
            return new CyclonMessage(subset);
        }
    };
}
