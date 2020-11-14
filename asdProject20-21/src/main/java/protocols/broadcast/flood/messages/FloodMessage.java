package protocols.broadcast.flood.messages;

import babel.generic.ProtoMessage;
import io.netty.buffer.ByteBuf;
import network.ISerializer;
import network.data.Host;
import utils.MessageIds;

import java.io.IOException;
import java.util.UUID;

public class FloodMessage extends ProtoMessage {
    public static final short MSG_ID = MessageIds.FLOOD_MESSAGE.getId();

    private final UUID mid;
    private final Host sender;

    private final short toDeliver;
    private final byte[] content;

    @Override
    public String toString() {
        return "FloodMessage{" +
                "mid=" + mid +
                '}';
    }

    public FloodMessage(UUID mid, Host sender, short toDeliver, byte[] content) {
        super(MSG_ID);
        this.mid = mid;
        this.sender = sender;
        this.toDeliver = toDeliver;
        this.content = content;
    }

    public Host getSender() {
        return sender;
    }

    public UUID getMid() {
        return mid;
    }

    public short getToDeliver() {
        return toDeliver;
    }

    public byte[] getContent() {
        return content;
    }

    public static ISerializer<FloodMessage> serializer = new ISerializer<>() {
        @Override
        public void serialize(FloodMessage floodMessage, ByteBuf out) throws IOException {
            out.writeLong(floodMessage.mid.getMostSignificantBits());
            out.writeLong(floodMessage.mid.getLeastSignificantBits());
            Host.serializer.serialize(floodMessage.sender, out);
            out.writeShort(floodMessage.toDeliver);
            out.writeInt(floodMessage.content.length);
            if (floodMessage.content.length > 0) {
                out.writeBytes(floodMessage.content);
            }
        }

        @Override
        public FloodMessage deserialize(ByteBuf in) throws IOException {
            long firstLong = in.readLong();
            long secondLong = in.readLong();
            UUID mid = new UUID(firstLong, secondLong);
            Host sender = Host.serializer.deserialize(in);
            short toDeliver = in.readShort();
            int size = in.readInt();
            byte[] content = new byte[size];
            if (size > 0)
                in.readBytes(content);

            return new FloodMessage(mid, sender, toDeliver, content);
        }
    };
}
