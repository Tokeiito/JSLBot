package net.coagulate.JSLBot.Packets.Messages;
import java.util.*;
import net.coagulate.JSLBot.Packets.*;
import net.coagulate.JSLBot.Packets.Types.*;
public class RpcChannelReply_bDataBlock extends Block {
	@Sequence(0)
	public LLUUID vtaskid=new LLUUID();
	@Sequence(1)
	public LLUUID vitemid=new LLUUID();
	@Sequence(2)
	public LLUUID vchannelid=new LLUUID();
}
