package net.coagulate.JSLBot.Packets.Messages;
import java.util.*;
import net.coagulate.JSLBot.Packets.*;
import net.coagulate.JSLBot.Packets.Types.*;
public class DirPlacesReply_bStatusData extends Block {
	@Sequence(0)
	public U32 vstatus=new U32();
}
