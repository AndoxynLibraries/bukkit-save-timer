/*
 * Copyright (C) 2013 Dabo Ross <http://www.daboross.net/>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.daboross.bukkitdev.bukkitsavetimer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

public class SaveTimer {

    private final Plugin plugin;
    private final long intervalSeconds;
    private final Runnable saveTask;
    private final AtomicBoolean taskScheduled;
    private final boolean runTaskAsync;
    private int bukkitTaskId = -1;

    public SaveTimer(Plugin plugin, Runnable saveTask, TimeUnit intervalUnit, long interval, boolean runTaskAsync) {
        this.plugin = plugin;
        this.saveTask = saveTask;
        this.intervalSeconds = intervalUnit.toSeconds(interval);
        this.taskScheduled = new AtomicBoolean(false);
        this.runTaskAsync = runTaskAsync;
        this.startTask();
    }

    public void cancelTask() {
        if (bukkitTaskId != -1) {
            BukkitScheduler sc = plugin.getServer().getScheduler();
            sc.cancelTask(bukkitTaskId);
            bukkitTaskId = -1;
        }
    }

    private void startTask() {
        BukkitScheduler sc = plugin.getServer().getScheduler();
        long ticks = 20 * intervalSeconds;
        if (runTaskAsync) {
            bukkitTaskId = sc.runTaskLaterAsynchronously(plugin, new SaveTimerRunnable(), ticks).getTaskId();
        } else {
            bukkitTaskId = sc.runTaskLater(plugin, new SaveTimerRunnable(), ticks).getTaskId();
        }
    }

    public long getInterval(TimeUnit unit) {
        return unit.convert(intervalSeconds, TimeUnit.SECONDS);
    }

    public void dataChanged() {
        if (taskScheduled.compareAndSet(false, true)
                || !plugin.getServer().getScheduler().isCurrentlyRunning(bukkitTaskId)) {
            startTask();
        }
    }

    private class SaveTimerRunnable implements Runnable {

        @Override
        public void run() {
            if (taskScheduled.compareAndSet(true, false)) {
                saveTask.run();
            }
        }
    }
}
