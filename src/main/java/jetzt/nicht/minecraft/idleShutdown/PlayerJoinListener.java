package jetzt.nicht.minecraft.idleShutdown;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import jetzt.nicht.minecraft.idleShutdown.IdleShutdown;

public class PlayerJoinListener implements Listener {
	IdleShutdown mainPlugin;

	public PlayerJoinListener(IdleShutdown mainPlugin) {
		this.mainPlugin = mainPlugin;
	}

	// Set lowest event priority to execute as late as possible. The idea being
	// that other plugins might modify player events to hide "non-player" joins,
	// kick players immediately or whatever, and we want to see the end result
	// of all that, not the intermediate steps.
	// This is just a guess how to behave, and might be totally wrong. Thus, it
	// might also be changed in the future.
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerJoin(PlayerJoinEvent event) {
		this.mainPlugin.onPlayerJoin();
	}
}
