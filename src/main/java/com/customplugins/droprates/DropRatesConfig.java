package com.customplugins.droprates;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.ui.ColorScheme;

@ConfigGroup("droprates")
public interface DropRatesConfig extends Config
{
	String LAYOUT_SECTION = "layoutSection";
	String APPEARANCE_SECTION = "appearanceSection";
	String COLLECTION_SECTION = "collectionSection";

	@ConfigSection(
		name = "Layout & sorting",
		description = "Category order, sorting, and hiding entries",
		position = 0
	)
	String layoutSection = LAYOUT_SECTION;

	@ConfigSection(
		name = "Appearance",
		description = "Fonts and colors",
		position = 1,
		closedByDefault = true
	)
	String appearanceSection = APPEARANCE_SECTION;

	@ConfigSection(
		name = "Collection tracking",
		description = "Mark drops as already collected",
		position = 2,
		closedByDefault = true
	)
	String collectionSection = COLLECTION_SECTION;

	@ConfigItem(
		keyName = "collapseCategoriesByDefault",
		name = "Collapse categories by default",
		description = "Start with every category collapsed until you click to expand it",
		position = 0,
		section = LAYOUT_SECTION
	)
	default boolean collapseCategoriesByDefault()
	{
		return true;
	}

	@ConfigItem(
		keyName = "sortMode",
		name = "Sort bosses by",
		description = "How bosses are ordered within each category (or the whole list, if merged)",
		position = 1,
		section = LAYOUT_SECTION
	)
	default SortMode sortMode()
	{
		return SortMode.DEFAULT;
	}

	@ConfigItem(
		keyName = "mergeAllCategories",
		name = "Merge all categories",
		description = "Show a single flat list instead of grouping by category",
		position = 2,
		section = LAYOUT_SECTION
	)
	default boolean mergeAllCategories()
	{
		return false;
	}

	@ConfigItem(
		keyName = "categoryOrder",
		name = "Category order override",
		description = "Comma-separated category names, in the order you want them shown. Unlisted categories keep the default order and are shown after these.",
		position = 3,
		section = LAYOUT_SECTION
	)
	default String categoryOrder()
	{
		return "";
	}

	@ConfigItem(
		keyName = "hiddenCategories",
		name = "Hidden categories",
		description = "Comma-separated category names to hide entirely",
		position = 4,
		section = LAYOUT_SECTION
	)
	default String hiddenCategories()
	{
		return "";
	}

	@ConfigItem(
		keyName = "hiddenBosses",
		name = "Hidden bosses",
		description = "Comma-separated boss names to hide entirely",
		position = 5,
		section = LAYOUT_SECTION
	)
	default String hiddenBosses()
	{
		return "";
	}

	@ConfigItem(
		keyName = "fontChoice",
		name = "Font",
		description = "Font used throughout the panel",
		position = 0,
		section = APPEARANCE_SECTION
	)
	default FontChoice fontChoice()
	{
		return FontChoice.RUNELITE_DEFAULT;
	}

	@ConfigItem(
		keyName = "accentColor",
		name = "Titles & category names",
		description = "Color for section titles, category headers, and combined/emphasized rates",
		position = 1,
		section = APPEARANCE_SECTION
	)
	default Color accentColor()
	{
		return ColorScheme.BRAND_ORANGE;
	}

	@ConfigItem(
		keyName = "itemNameColor",
		name = "Item & boss names",
		description = "Color for item names and boss names in the list",
		position = 2,
		section = APPEARANCE_SECTION
	)
	default Color itemNameColor()
	{
		return ColorScheme.TEXT_COLOR;
	}

	@ConfigItem(
		keyName = "rateColor",
		name = "Drop rate numbers",
		description = "Base color for drop rate numbers (when not color-coded by shared rate)",
		position = 3,
		section = APPEARANCE_SECTION
	)
	default Color rateColor()
	{
		return ColorScheme.LIGHT_GRAY_COLOR;
	}

	@ConfigItem(
		keyName = "colorCodeSharedRates",
		name = "Color-code shared drop rates",
		description = "Give items that share the same drop rate a matching muted color",
		position = 4,
		section = APPEARANCE_SECTION
	)
	default boolean colorCodeSharedRates()
	{
		return true;
	}

	@ConfigItem(
		keyName = "enableCollectionTracking",
		name = "Enable collection tracking",
		description = "Show a checkbox next to each drop so you can mark it as already collected",
		position = 0,
		section = COLLECTION_SECTION
	)
	default boolean enableCollectionTracking()
	{
		return false;
	}

	@ConfigItem(
		keyName = "collectedColor",
		name = "Collected item color",
		description = "Color used for items you've marked as collected",
		position = 1,
		section = COLLECTION_SECTION
	)
	default Color collectedColor()
	{
		return new Color(0x7F, 0xB3, 0x7F);
	}
}
