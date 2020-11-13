package protocols.broadcast.plumtree.messages;

import babel.generic.ProtoMessage;
import io.netty.buffer.ByteBuf;
import network.ISerializer;
import network.data.Host;

import java.io.IOException;
import java.util.UUID;


public class IHaveMessage extends ProtoMessage  {
    public static final short MSG_ID = 203;

    private final UUID mid;
    private final Host sender;
    private final Host myself;

    private final int round;

    private final byte[] content;

    @Override
    public String toString() {
        return "IHaveMessage{" +
                "mid=" + mid +
                '}';
    }

    public IHaveMessage(UUID mid, Host sender, Host myself, int round, byte[] content) {
        super(MSG_ID);
        this.mid = mid;
        this.sender = sender;
        this.myself = myself;
        this.round = round;
        this.content = content;

    }


    public Host getSender() {
        return sender;
    }

    public UUID getMid() {
        return mid;
    }

    public Host getMyself() {
        return myself;
    }

    public byte[] getContent() {
        return content;
    }



    public static ISerializer<IHaveMessage> serializer = new ISerializer<>() {
        @Override
        public void serialize(IHaveMessage iHaveMessage, ByteBuf out) throws IOException {
            out.writeLong(iHaveMessage.mid.getMostSignificantBits());
            out.writeLong(iHaveMessage.mid.getLeastSignificantBits());
            Host.serializer.serialize(iHaveMessage.sender, out);
            Host.serializer.serialize(iHaveMessage.myself, out);
            out.writeInt(iHaveMessage.round);
            out.writeInt(iHaveMessage.content.length);
            if (iHaveMessage.content.length > 0) {
                out.writeBytes(iHaveMessage.content);
            }
        }

        @Override
        public IHaveMessage deserialize(ByteBuf in) throws IOException {
            long firstLong = in.readLong();
            long secondLong = in.readLong();
            UUID mid = new UUID(firstLong, secondLong);
            Host sender = Host.serializer.deserialize(in);
            Host myself = Host.serializer.deserialize(in);
            int round = in.readInt();
            int size = in.readInt();
            byte[] content = new byte[size];
            if (size > 0)
                in.readBytes(content);

            return new IHaveMessage(mid, sender, myself, round, content);
        }

    };

}





