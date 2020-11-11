package protocols.broadcast.eagerPushGossip.messages;
import babel.generic.ProtoMessage;
import io.netty.buffer.ByteBuf;
import network.ISerializer;
import network.data.Host;

import java.io.IOException;
import java.util.UUID;
public class EPGMessage extends ProtoMessage {

    public static final short MSG_ID = 202;

    private final UUID mid;
    private final Host sender;

    private final short toDeliver;
    private final byte[] content;

    @Override
    public String toString() {
        return "EPGMessage{" +
                "mid=" + mid +
                '}';
    }

    public EPGMessage(UUID mid, Host sender, short toDeliver, byte[] content) {
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

    public static ISerializer<EPGMessage> serializer = new ISerializer<>() {
        @Override
        public void serialize(EPGMessage epgMessage, ByteBuf out) throws IOException {
            out.writeLong(epgMessage.mid.getMostSignificantBits());
            out.writeLong(epgMessage.mid.getLeastSignificantBits());
            Host.serializer.serialize(epgMessage.sender, out);
            out.writeShort(epgMessage.toDeliver);
            out.writeInt(epgMessage.content.length);
            if (epgMessage.content.length > 0) {
                out.writeBytes(epgMessage.content);
            }
        }

        @Override
        public EPGMessage deserialize(ByteBuf in) throws IOException {
            long firstLong = in.readLong();
            long secondLong = in.readLong();
            UUID mid = new UUID(firstLong, secondLong);
            Host sender = Host.serializer.deserialize(in);
            short toDeliver = in.readShort();
            int size = in.readInt();
            byte[] content = new byte[size];
            if (size > 0)
                in.readBytes(content);

            return new EPGMessage(mid, sender, toDeliver, content);
        }
    };
}
