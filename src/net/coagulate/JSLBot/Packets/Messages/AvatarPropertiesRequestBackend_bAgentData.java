package net.coagulate.JSLBot.Packets.Messages;
import java.util.*;
import net.coagulate.JSLBot.Packets.*;
import net.coagulate.JSLBot.Packets.Types.*;
public class AvatarPropertiesRequestBackend_bAgentData extends Block {
	@Sequence(0)
	public LLUUID vagentid=new LLUUID();
	@Sequence(1)
	public LLUUID vavatarid=new LLUUID();
	@Sequence(2)
	public U8 vgodlevel=new U8();
	@Sequence(3)
	public BOOL vwebprofilesdisabled=new BOOL();
}
