package net.coagulate.JSLBot.Packets.Messages;
import java.util.*;
import net.coagulate.JSLBot.JSLBot;
import net.coagulate.JSLBot.Packets.*;
import net.coagulate.JSLBot.Packets.Types.*;
public class SimStats extends Block implements Message {
	public final int getFrequency() { return Frequency.LOW; }
	public final int getId() { return 140; }
	public final String getName() { return "SimStats"; }
	@Sequence(0)
	public SimStats_bRegion bregion=new SimStats_bRegion();
	@Sequence(1)
	public List<SimStats_bStat> bstat;
	@Sequence(2)
	public SimStats_bPidStat bpidstat=new SimStats_bPidStat();
    public SimStats_bStat getStat(SimStat stat) {
        SimStat[] lookup = SimStat.values();
        for (SimStats_bStat astat:bstat) {
            if (lookup[astat.vstatid.value]==stat) {
                return astat;
            }
        }
        return null;
    }
    public enum SimStat {
            TIME_DILATION(0),
            FPS(1),
            PHYSFPS(2),
            AGENTUPS(3),
            FRAMEMS(4),
            NETMS(5),
            SIMOTHERMS(6),
            SIMPHYSICSMS(7),
            AGENTMS(8),
            IMAGESMS(9),
            SCRIPTMS(10),
            NUMTASKS(11),
            NUMTASKSACTIVE(12),
            NUMAGENTMAIN(13),
            NUMAGENTCHILD(14),
            NUMSCRIPTSACTIVE(15),
            LSLIPS(16),
            INPPS(17),
            OUTPPS(18),
            PENDING_DOWNLOADS(19),
            PENDING_UPLOADS(20),
            VIRTUAL_SIZE_KB(21),
            RESIDENT_SIZE_KB(22),
            PENDING_LOCAL_UPLOADS(23),
            TOTAL_UNACKED_BYTES(24),
            PHYSICS_PINNED_TASKS(25),
            PHYSICS_LOD_TASKS(26),
            SIMPHYSICSSTEPMS(27),
            SIMPHYSICSSHAPEMS(28),
            SIMPHYSICSOTHERMS(29),
            SIMPHYSICSMEMORY(30),
            SCRIPT_EPS(31),
            SIMSPARETIME(32),
            SIMSLEEPTIME(33),
            IOPUMPTIME(34),
            PCTSCRIPTSRUN(35),
            REGION_IDLE(36), // dataserver only
            REGION_IDLE_POSSIBLE(37), // dataserver only
            SIMAISTEPTIMEMS(38),
            SKIPPEDAISILSTEPS_PS(39),
            PCTSTEPPEDCHARACTERS(40);
            private int value;
            private SimStat(int value) {
                    this.value = value;
            }
    }

}
