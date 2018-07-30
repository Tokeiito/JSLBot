package net.coagulate.JSLBot.Packets.Messages;
import java.util.*;
import net.coagulate.JSLBot.Packets.*;
import net.coagulate.JSLBot.Packets.Types.*;
public class RpcChannelRequest_bDataBlock extends Block {
	@Sequence(0)
	public U32 vgridx=new U32();
	@Sequence(1)
	public U32 vgridy=new U32();
	@Sequence(2)
	public LLUUID vtaskid=new LLUUID();
	@Sequence(3)
	public LLUUID vitemid=new LLUUID();
}
