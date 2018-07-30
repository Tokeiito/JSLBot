package net.coagulate.JSLBot.Packets.Messages;
import java.util.*;
import net.coagulate.JSLBot.Packets.*;
import net.coagulate.JSLBot.Packets.Types.*;
public class CompleteAgentMovement extends Block implements Message {
	public final int getFrequency() { return Frequency.LOW; }
	public final int getId() { return 249; }
	public final String getName() { return "CompleteAgentMovement"; }
	@Sequence(0)
	public CompleteAgentMovement_bAgentData bagentdata=new CompleteAgentMovement_bAgentData();
}
