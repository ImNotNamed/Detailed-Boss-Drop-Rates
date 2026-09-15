package com.customplugins.droprates;

import com.google.gson.Gson;
import java.awt.Color;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.PluginPanel;

/**
 * Exercises the DropRatesConfig options (merge, sort modes, hidden categories/bosses,
 * collection tracking, custom fonts/colors) against fixed scenarios, without needing to
 * click through the real client. Not part of the shipped plugin.
 */
public class ConfigVariantsHarness
{
	public static void main(String[] args) throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			try
			{
				render();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		});
		System.exit(0);
	}

	private static void render() throws IOException
	{
		File outDir = new File("screenshots-config");
		outDir.mkdirs();
		for (File f : outDir.listFiles())
		{
			f.delete();
		}

		capture(outDir, "expanded-by-default", new FakeDropRatesConfig()
		{
			@Override
			public boolean collapseCategoriesByDefault()
			{
				return false;
			}
		}, new FakeConfigStore(), null);

		capture(outDir, "merged-alphabetical", new FakeDropRatesConfig()
		{
			@Override
			public boolean mergeAllCategories()
			{
				return true;
			}

			@Override
			public SortMode sortMode()
			{
				return SortMode.ALPHABETICAL;
			}

			@Override
			public boolean collapseCategoriesByDefault()
			{
				return false;
			}
		}, new FakeConfigStore(), null);

		capture(outDir, "combat-level-sort-wilderness", new FakeDropRatesConfig()
		{
			@Override
			public SortMode sortMode()
			{
				return SortMode.COMBAT_LEVEL;
			}

			@Override
			public boolean collapseCategoriesByDefault()
			{
				return false;
			}

			@Override
			public String categoryOrder()
			{
				return "Wilderness Bosses,Revenants";
			}
		}, new FakeConfigStore(), null);

		capture(outDir, "hidden-categories-and-bosses", new FakeDropRatesConfig()
		{
			@Override
			public String hiddenCategories()
			{
				return "Superior Creatures, Revenants";
			}

			@Override
			public String hiddenBosses()
			{
				return "Nex, Zulrah";
			}

			@Override
			public boolean collapseCategoriesByDefault()
			{
				return false;
			}
		}, new FakeConfigStore(), null);

		capture(outDir, "custom-fonts-and-colors", new FakeDropRatesConfig()
		{
			@Override
			public FontChoice fontChoice()
			{
				return FontChoice.MONOSPACED;
			}

			@Override
			public Color accentColor()
			{
				return new Color(0x4A, 0x90, 0xD9);
			}

			@Override
			public Color itemNameColor()
			{
				return new Color(0xE0, 0xC0, 0x80);
			}

			@Override
			public Color rateColor()
			{
				return new Color(0xFF, 0x80, 0x80);
			}

			@Override
			public boolean colorCodeSharedRates()
			{
				return false;
			}
		}, new FakeConfigStore(), "general-graardor");

		FakeConfigStore preCollected = new FakeConfigStore();
		preCollected.set("collected_general-graardor_bandos_hilt", "true");
		capture(outDir, "collection-tracking", new FakeDropRatesConfig()
		{
			@Override
			public boolean enableCollectionTracking()
			{
				return true;
			}
		}, preCollected, "general-graardor");
	}

	private static void capture(File outDir, String name, DropRatesConfig config, ConfigStore configStore, String bossId) throws IOException
	{
		Gson gson = new Gson();
		DropDataService service = new DropDataService(gson);
		DropRatesPanel panel = new DropRatesPanel(service, config, configStore);

		JFrame frame = new JFrame();
		frame.setUndecorated(true);
		frame.setLocation(-3000, -3000);
		frame.add(panel);
		frame.setSize(PluginPanel.PANEL_WIDTH + 20, 2400);
		frame.setVisible(true);
		frame.validate();

		if (bossId != null)
		{
			BossData data = service.getAll().stream().filter(b -> b.getId().equals(bossId)).findFirst().orElseThrow();
			panel.showEntry(data);
			frame.validate();
		}

		BufferedImage img = capture(panel);
		ImageIO.write(img, "png", new File(outDir, name + ".png"));

		frame.dispose();
	}

	private static BufferedImage capture(Component component) throws IOException
	{
		int width = component.getWidth();
		int height = Math.max(component.getHeight(), 1);
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		component.paint(img.getGraphics());
		return img;
	}
}
