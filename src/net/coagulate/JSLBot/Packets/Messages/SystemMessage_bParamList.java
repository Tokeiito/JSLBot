package net.coagulate.JSLBot.Packets.Messages;
import java.util.*;
import net.coagulate.JSLBot.Packets.*;
import net.coagulate.JSLBot.Packets.Types.*;
public class SystemMessage_bParamList extends Block {
	@Sequence(0)
	public Variable1 vparameter=new Variable1();
}
