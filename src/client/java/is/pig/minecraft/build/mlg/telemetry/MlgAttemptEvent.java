package is.pig.minecraft.build.mlg.telemetry;

import is.pig.minecraft.lib.util.perf.PerfMonitor;
import is.pig.minecraft.lib.util.telemetry.StructuredEvent;
import net.minecraft.client.Minecraft;
import org.slf4j.event.Level;

import java.util.Map;

/**
 * Structured event captured when an MLG survival attempt is initiated.
 */
public record MlgAttemptEvent(
        long timestamp,
        long tick,
        Level level,
        double tps,
        double mspt,
        double cps,
        String pos,
        float fallDistance,
        String methodName,
        boolean methodFound,
        boolean isFatal,
        int impactTicks
) implements StructuredEvent {

    /**
     * Creates a new MlgAttemptEvent with current environmental metrics.
     */
    public MlgAttemptEvent(float fallDistance, String methodName, boolean methodFound, boolean isFatal, int impactTicks) {
        this(
                System.currentTimeMillis(),
                Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0,
                Level.INFO,
                PerfMonitor.getInstance().getServerTps(),
                PerfMonitor.getInstance().getClientMspt(),
                PerfMonitor.getInstance().getCps(),
                Minecraft.getInstance().player != null ? 
                    String.format("(%.1f,%.1f,%.1f)", Minecraft.getInstance().player.getX(), Minecraft.getInstance().player.getY(), Minecraft.getInstance().player.getZ()) : "n/a",
                fallDistance,
                methodName,
                methodFound,
                isFatal,
                impactTicks
        );
    }

    @Override
    public boolean isFailure() {
        // High-signal failure: Method was found but execution was fatal
        return methodFound && isFatal;
    }

    @Override
    public String getCategoryIcon() {
        return "🪣";
    }

    @Override
    public String getEventKey() {
        return "piggy.build.telemetry.mlg_attempt";
    }

    @Override
    public Map<String, Object> getEventData() {
        return Map.of(
                "fallDistance", fallDistance,
                "methodName", methodName,
                "methodFound", methodFound,
                "isFatal", isFatal,
                "impactTicks", impactTicks
        );
    }

    @Override
    public String formatted() {
        String status = isFatal ? "FATAL" : "SUCCESS";
        if (!methodFound) status = "ABORTED (No method)";
        
        return String.format("[%d] [Tick:%d] [MLG] %s attempt from %.1f blocks. Status: %s, Impact in %d t", 
                timestamp, tick, methodName, fallDistance, status, impactTicks);
    }
}
