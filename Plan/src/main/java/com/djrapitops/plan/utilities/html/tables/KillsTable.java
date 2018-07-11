package com.djrapitops.plan.utilities.html.tables;

import com.djrapitops.plan.api.PlanAPI;
import com.djrapitops.plan.data.container.PlayerKill;
import com.djrapitops.plan.data.element.TableContainer;
import com.djrapitops.plan.data.store.mutators.formatting.Formatter;
import com.djrapitops.plan.data.store.mutators.formatting.Formatters;
import com.djrapitops.plan.data.store.objects.DateHolder;
import com.djrapitops.plan.system.cache.DataCache;
import com.djrapitops.plan.utilities.comparators.PlayerKillComparator;
import com.djrapitops.plan.utilities.html.Html;
import com.djrapitops.plan.utilities.html.icon.Family;
import com.djrapitops.plan.utilities.html.icon.Icon;

import java.util.Collections;
import java.util.List;

/**
 * @author Rsl1122
 */
public class KillsTable extends TableContainer {

    public KillsTable(List<PlayerKill> playerKills) {
        super(Icon.called("clock").of(Family.REGULAR) + " Time", "Killed", "With");
        setColor("red");
        
        if (playerKills.isEmpty()) {
            addRow("No Kills");
        } else {
            addValues(playerKills);
        }
    }

    private void addValues(List<PlayerKill> playerKills) {
        playerKills.sort(new PlayerKillComparator());
        Collections.reverse(playerKills);

        Formatter<DateHolder> timestamp = Formatters.year();

        int i = 0;
        DataCache dataCache = DataCache.getInstance();
        for (PlayerKill kill : playerKills) {
            if (i >= 20) {
                break;
            }

            String name = dataCache.getName(kill.getVictim());
            addRow(
                    timestamp.apply(kill),
                    Html.LINK.parse(PlanAPI.getInstance().getPlayerInspectPageLink(name), name),
                    kill.getWeapon()
            );

            i++;
        }
    }
}
