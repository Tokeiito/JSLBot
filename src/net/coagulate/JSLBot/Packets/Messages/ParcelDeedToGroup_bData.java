package net.coagulate.JSLBot.Packets.Messages;
import java.util.*;
import net.coagulate.JSLBot.Packets.*;
import net.coagulate.JSLBot.Packets.Types.*;
public class ParcelDeedToGroup_bData extends Block {
	@Sequence(0)
	public LLUUID vgroupid=new LLUUID();
	@Sequence(1)
	public S32 vlocalid=new S32();
}
