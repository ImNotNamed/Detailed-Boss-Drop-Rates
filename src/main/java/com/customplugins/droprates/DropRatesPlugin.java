package com.customplugins.droprates;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@Slf4j
@PluginDescriptor(
	name = "Unique Drop Rates",
	description = "Look up a monster's unique drop rates (individual and combined odds) before you go fight it",
	tags = {"drops", "rates", "uniques", "wiki", "loot"}
)
public class DropRatesPlugin extends Plugin
{
	private static final String MENU_OPTION = "View drop rates";
	private static final String CONFIG_GROUP = "droprates";

	@Inject
	private Client client;

	@Inject
	private DropDataService dropDataService;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private DropRatesConfig config;

	@Inject
	private ConfigManager configManager;

	private final Set<Integer> handledThisTick = new HashSet<>();

	private DropRatesPanel panel;
	private NavigationButton navButton;

	@Provides
	DropRatesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DropRatesConfig.class);
	}

	@Override
	protected void startUp()
	{
		ConfigStore configStore = new ConfigStore()
		{
			@Override
			public String get(String key)
			{
				return configManager.getConfiguration(CONFIG_GROUP, key);
			}

			@Override
			public void set(String key, String value)
			{
				configManager.setConfiguration(CONFIG_GROUP, key, value);
			}

			@Override
			public void unset(String key)
			{
				configManager.unsetConfiguration(CONFIG_GROUP, key);
			}
		};

		panel = new DropRatesPanel(dropDataService, config, configStore);

		navButton = NavigationButton.builder()
			.tooltip("Unique Drop Rates")
			.icon(createIcon())
			.priority(6)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navButton);
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals(CONFIG_GROUP))
		{
			panel.onConfigChanged();
		}
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		handledThisTick.clear();
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		NPC npc = event.getMenuEntry().getNpc();
		if (npc == null || npc.getName() == null || npc.getCombatLevel() <= 0)
		{
			return;
		}

		if (!handledThisTick.add(npc.getIndex()))
		{
			return;
		}

		BossData data = dropDataService.findByNpcName(npc.getName());
		if (data == null)
		{
			return;
		}

		client.getMenu().createMenuEntry(-1)
			.setOption(MENU_OPTION)
			.setTarget(event.getMenuEntry().getTarget())
			.setType(MenuAction.RUNELITE)
			.onClick(e -> openEntry(data));
	}

	private void openEntry(BossData data)
	{
		clientToolbar.openPanel(navButton);
		panel.showEntry(data);
	}

	private static BufferedImage createIcon()
	{
		BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = icon.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(new Color(220, 138, 0));
		g.fillOval(0, 0, 16, 16);
		g.setColor(Color.WHITE);
		g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
		g.drawString("%", 3, 12);
		g.dispose();
		return icon;
	}
}
