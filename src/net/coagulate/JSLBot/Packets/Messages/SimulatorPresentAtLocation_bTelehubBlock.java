package net.coagulate.JSLBot.Packets.Messages;
import java.util.*;
import net.coagulate.JSLBot.Packets.*;
import net.coagulate.JSLBot.Packets.Types.*;
public class SimulatorPresentAtLocation_bTelehubBlock extends Block {
	@Sequence(0)
	public BOOL vhastelehub=new BOOL();
	@Sequence(1)
	public LLVector3 vtelehubpos=new LLVector3();
}
