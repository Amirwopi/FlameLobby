package ir.mrwopi.flameLobby.tasks;


import ir.mrwopi.flameLobby.FlameLobby;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.Calendar;
import java.util.TimeZone;

public class LobbyTimeTask extends BukkitRunnable {
    private final FlameLobby plugin;

    public LobbyTimeTask(FlameLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        try {
            World world = Bukkit.getWorld("spawn");
            if (world == null) return;

            Boolean cycle = world.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE);
            if (cycle == null || cycle) {
                world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            }

            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"));
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            int minute = cal.get(Calendar.MINUTE);

            long mcTime = calculateMinecraftTime(hour, minute);
            world.setTime(mcTime);

        } catch (Exception e) {
            plugin.getLogger().severe("LobbyTimeTask error: " + e.getMessage());
        }
    }

    private long calculateMinecraftTime(int hour, int minute) {
        int totalMinutes = (hour * 60) + minute;

        int sunriseMinutes = 6 * 60 + 26;
        int sunsetMinutes = 17 * 60 + 9;
        int endOfDayMinutes = 24 * 60;

        long mcSunset = 12000L;
        long mcMidnight = 18000L;
        long mcDayEnd = 24000L;

        if (totalMinutes >= sunriseMinutes && totalMinutes < sunsetMinutes) {
            int dayMinutes = sunsetMinutes - sunriseMinutes;
            int minutesSinceSunrise = totalMinutes - sunriseMinutes;
            return (minutesSinceSunrise * mcSunset) / dayMinutes;
        }

        if (totalMinutes >= sunsetMinutes) {
            int nightMinutes = endOfDayMinutes - sunsetMinutes;
            int minutesSinceSunset = totalMinutes - sunsetMinutes;
            return mcSunset + ((minutesSinceSunset * (mcMidnight - mcSunset)) / nightMinutes);
        }

        int nightProgress = (totalMinutes * (int)(mcDayEnd - mcMidnight)) / sunriseMinutes;
        return mcMidnight + nightProgress;
    }
}