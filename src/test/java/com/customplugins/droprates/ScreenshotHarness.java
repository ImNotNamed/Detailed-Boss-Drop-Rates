package com.customplugins.droprates;

import com.google.gson.Gson;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.PluginPanel;

/**
 * Renders the plugin's panel to PNG files without launching the game client,
 * for fast visual iteration during development. Not part of the shipped plugin.
 */
public class ScreenshotHarness
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
		Gson gson = new Gson();
		DropDataService service = new DropDataService(gson);
		DropRatesPanel panel = new DropRatesPanel(service, new FakeDropRatesConfig(), new FakeConfigStore());

		JFrame frame = new JFrame();
		frame.setUndecorated(true);
		frame.setLocation(-3000, -3000);
		frame.add(panel);
		frame.setSize(PluginPanel.PANEL_WIDTH + 20, 2400);
		frame.setVisible(true);
		frame.validate();

		File outDir = new File("screenshots");
		outDir.mkdirs();
		for (File f : outDir.listFiles())
		{
			f.delete();
		}

		capture(panel, new File(outDir, "00-list.png"));

		panel.searchForTesting("shard");
		frame.validate();
		capture(panel, new File(outDir, "00-search-shard.png"));
		panel.searchForTesting("");

		for (BossData data : service.getAll())
		{
			panel.showEntry(data);
			frame.validate();
			capture(panel, new File(outDir, data.getId() + ".png"));
		}

		frame.dispose();
	}

	private static void capture(Component component, File file) throws IOException
	{
		int width = component.getWidth();
		int height = Math.max(component.getHeight(), 1);
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		component.paint(img.getGraphics());
		ImageIO.write(img, "png", file);
	}
}
