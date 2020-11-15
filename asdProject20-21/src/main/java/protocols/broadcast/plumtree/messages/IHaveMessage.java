package protocols.broadcast.plumtree.messages;

import babel.generic.ProtoMessage;
import io.netty.buffer.ByteBuf;
import network.ISerializer;
import network.data.Host;
import utils.MessageIds;

import java.io.IOException;
import java.util.UUID;


public class IHaveMessage extends ProtoMessage  {
    public static final short MSG_ID = MessageIds.I_HAVE_MESSAGE.getId();

    private final UUID mid;
    private final Host sender;
    private final byte[] content;

    @Override
    public String toString() {
        return "IHaveMessage{" +
                "mid=" + mid +
                '}';
    }

    public IHaveMessage(UUID mid, Host sender, byte[] content) {
        super(MSG_ID);
        this.mid = mid;
        this.sender = sender;
        this.content = content;
    }


    public Host getSender() {
        return sender;
    }

    public UUID getMid() {
        return mid;
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
            int size = in.readInt();
            byte[] content = new byte[size];
            if (size > 0)
                in.readBytes(content);

            return new IHaveMessage(mid, sender, content);
        }

    };

}





