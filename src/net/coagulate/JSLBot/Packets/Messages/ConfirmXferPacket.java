package net.coagulate.JSLBot.Packets.Messages;
import java.util.*;
import net.coagulate.JSLBot.Packets.*;
import net.coagulate.JSLBot.Packets.Types.*;
public class ConfirmXferPacket extends Block implements Message {
	public final int getFrequency() { return Frequency.HIGH; }
	public final int getId() { return 19; }
	public final String getName() { return "ConfirmXferPacket"; }
	@Sequence(0)
	public ConfirmXferPacket_bXferID bxferid=new ConfirmXferPacket_bXferID();
}
