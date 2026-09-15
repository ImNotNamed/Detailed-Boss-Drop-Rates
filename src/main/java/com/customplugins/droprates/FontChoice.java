package com.customplugins.droprates;

import java.awt.Font;
import net.runelite.client.ui.FontManager;

public enum FontChoice
{
	RUNELITE_DEFAULT("RuneLite default")
		{
			@Override
			Font base()
			{
				return FontManager.getDefaultFont();
			}
		},
	RUNESCAPE("RuneScape")
		{
			@Override
			Font base()
			{
				return FontManager.getRunescapeFont();
			}
		},
	RUNESCAPE_SMALL("RuneScape (small)")
		{
			@Override
			Font base()
			{
				return FontManager.getRunescapeSmallFont();
			}
		},
	SANS_SERIF("Sans-serif")
		{
			@Override
			Font base()
			{
				return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
			}
		},
	SERIF("Serif")
		{
			@Override
			Font base()
			{
				return new Font(Font.SERIF, Font.PLAIN, 12);
			}
		},
	MONOSPACED("Monospaced")
		{
			@Override
			Font base()
			{
				return new Font(Font.MONOSPACED, Font.PLAIN, 12);
			}
		};

	private final String label;

	FontChoice(String label)
	{
		this.label = label;
	}

	abstract Font base();

	Font derive(int style, float size)
	{
		return base().deriveFont(style, size);
	}

	@Override
	public String toString()
	{
		return label;
	}
}
