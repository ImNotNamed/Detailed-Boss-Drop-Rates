package com.customplugins.droprates;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.util.LinkBrowser;

class DropRatesPanel extends PluginPanel
{
	private static final NumberFormat NUMBER_FORMAT = NumberFormat.getIntegerInstance();
	private static final int TEXT_WIDTH_PX = 165;

	// Default display order for known categories; anything else (or null) is grouped
	// under "Other" and shown after these. Overridable via DropRatesConfig#categoryOrder().
	private static final List<String> CATEGORY_ORDER = List.of(
		"God Wars Dungeon", "Raids", "Skilling Bosses", "Wilderness Bosses", "Minigame", "Slayer Bosses",
		"Other Bosses", "Sporadic", "Demons & Gorillas", "Revenants", "Superior Creatures");

	// Categories whose entries are alphabetical (rather than JSON insertion order) when the
	// sort mode is DEFAULT. ALPHABETICAL/COMBAT_LEVEL sort modes apply to every category.
	private static final Set<String> ALPHABETICAL_CATEGORIES = Set.of("Other Bosses");

	private final DropDataService dropDataService;
	private final DropRatesConfig config;
	private final ConfigStore configStore;
	private final CardLayout cardLayout = new CardLayout();
	private final JPanel cards = new JPanel(cardLayout);
	// Elements are either a String (category header, not selectable) or a BossData (a row).
	private final DefaultListModel<Object> listModel = new DefaultListModel<>();
	private final JPanel detailPanel = new JPanel();
	private final JList<Object> list = new JList<>(listModel);
	private final JScrollPane listScroll = new JScrollPane(list);
	private String lastQuery = "";
	private BossData lastShownEntry;

	DropRatesPanel(DropDataService dropDataService, DropRatesConfig config, ConfigStore configStore)
	{
		super(false);
		this.dropDataService = dropDataService;
		this.config = config;
		this.configStore = configStore;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		IconTextField searchField = new IconTextField();
		searchField.setIcon(IconTextField.Icon.SEARCH);
		searchField.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH, 30));
		searchField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		searchField.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		searchField.getDocument().addDocumentListener((SimpleDocListener) () -> populateList(searchField.getText()));
		searchField.addClearListener(() -> populateList(""));

		list.setBackground(ColorScheme.DARK_GRAY_COLOR);
		list.setCellRenderer((jList, value, index, isSelected, cellHasFocus) ->
		{
			if (value instanceof String)
			{
				String category = (String) value;
				String arrow = isCollapsed(category) ? "▸" : "▾";
				JLabel header = new JLabel(arrow + "  " + category.toUpperCase());
				header.setFont(categoryFont());
				header.setForeground(accentColor());
				header.setOpaque(true);
				header.setBackground(ColorScheme.DARK_GRAY_COLOR);
				header.setBorder(new EmptyBorder(10, 10, 4, 10));
				return header;
			}

			BossData data = (BossData) value;
			JLabel label = new JLabel(data.getDisplayName());
			label.setFont(itemFont());
			label.setBorder(new EmptyBorder(8, 18, 8, 10));
			label.setOpaque(true);
			label.setBackground(isSelected ? ColorScheme.DARK_GRAY_HOVER_COLOR : ColorScheme.DARKER_GRAY_COLOR);
			label.setForeground(itemNameColor());
			return label;
		});
		list.addListSelectionListener(e ->
		{
			if (e.getValueIsAdjusting())
			{
				return;
			}

			Object selected = list.getSelectedValue();
			if (selected instanceof BossData)
			{
				showEntry((BossData) selected);
			}
			else if (selected instanceof String)
			{
				toggleCollapsed((String) selected);
				list.clearSelection();
			}
			else if (selected != null)
			{
				list.clearSelection();
			}
		});

		JPanel listWrapper = new JPanel(new BorderLayout());
		listWrapper.setBorder(new EmptyBorder(8, 8, 8, 8));
		listWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		listWrapper.add(searchField, BorderLayout.NORTH);

		listScroll.setBorder(new EmptyBorder(8, 0, 0, 0));
		listWrapper.add(listScroll, BorderLayout.CENTER);

		JButton backButton = new JButton("← Back to search");
		backButton.setFocusPainted(false);
		backButton.addActionListener(e ->
		{
			list.clearSelection();
			cardLayout.show(cards, "list");
		});
		JPanel backWrapper = new JPanel(new BorderLayout());
		backWrapper.setBorder(new EmptyBorder(8, 8, 4, 8));
		backWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		backWrapper.add(backButton, BorderLayout.CENTER);

		detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
		detailPanel.setBorder(new EmptyBorder(4, 8, 8, 8));
		detailPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JScrollPane detailScroll = new JScrollPane(detailPanel);
		detailScroll.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel detailCard = new JPanel(new BorderLayout());
		detailCard.setBackground(ColorScheme.DARK_GRAY_COLOR);
		detailCard.add(backWrapper, BorderLayout.NORTH);
		detailCard.add(detailScroll, BorderLayout.CENTER);

		cards.add(listWrapper, "list");
		cards.add(detailCard, "detail");

		add(cards, BorderLayout.CENTER);

		populateList("");
	}

	/** Test-only hook so the offline screenshot harness can exercise search without a real text field event. */
	void searchForTesting(String query)
	{
		populateList(query);
	}

	private void populateList(String query)
	{
		lastQuery = query;
		listModel.clear();
		List<BossData> results = dropDataService.search(query);

		Set<String> hiddenCategories = parseCsvLower(config.hiddenCategories());
		Set<String> hiddenBosses = parseCsvLower(config.hiddenBosses());

		List<BossData> filtered = new ArrayList<>();
		for (BossData data : results)
		{
			if (hiddenCategories.contains(categoryOf(data).toLowerCase())
				|| hiddenBosses.contains(data.getDisplayName().toLowerCase()))
			{
				continue;
			}
			filtered.add(data);
		}

		boolean merged = config.mergeAllCategories();

		// When merged with a real sort mode, the sort applies across the whole flat list
		// rather than within each category's boundary (which would defeat the point of merging).
		if (merged && config.sortMode() != SortMode.DEFAULT)
		{
			List<BossData> allBosses = new ArrayList<>(filtered);
			sortBosses(allBosses, null);
			for (BossData data : allBosses)
			{
				listModel.addElement(data);
			}
			return;
		}

		List<String> order = resolveCategoryOrder(filtered);
		List<BossData> mergedList = merged ? new ArrayList<>() : null;

		for (String category : order)
		{
			List<BossData> bosses = new ArrayList<>();
			for (BossData data : filtered)
			{
				if (categoryOf(data).equals(category))
				{
					bosses.add(data);
				}
			}
			if (bosses.isEmpty())
			{
				continue;
			}

			sortBosses(bosses, category);

			if (merged)
			{
				mergedList.addAll(bosses);
			}
			else
			{
				addCategorySection(category, bosses);
			}
		}

		if (merged)
		{
			for (BossData data : mergedList)
			{
				listModel.addElement(data);
			}
		}
	}

	private static String categoryOf(BossData data)
	{
		return data.getCategory() == null ? "Other" : data.getCategory();
	}

	private static Set<String> parseCsvLower(String csv)
	{
		Set<String> set = new java.util.HashSet<>();
		if (csv == null || csv.isBlank())
		{
			return set;
		}
		for (String part : csv.split(","))
		{
			String trimmed = part.trim();
			if (!trimmed.isEmpty())
			{
				set.add(trimmed.toLowerCase());
			}
		}
		return set;
	}

	/** Category order: user override first (matched case-insensitively against real category
	 * names), then the curated default order, then any leftover categories in first-seen order. */
	private List<String> resolveCategoryOrder(List<BossData> filtered)
	{
		java.util.Map<String, String> canonicalByLower = new java.util.LinkedHashMap<>();
		for (String c : CATEGORY_ORDER)
		{
			canonicalByLower.putIfAbsent(c.toLowerCase(), c);
		}
		for (BossData data : filtered)
		{
			canonicalByLower.putIfAbsent(categoryOf(data).toLowerCase(), categoryOf(data));
		}

		LinkedHashSet<String> order = new LinkedHashSet<>();
		String overrideCsv = config.categoryOrder();
		if (overrideCsv != null && !overrideCsv.isBlank())
		{
			for (String part : overrideCsv.split(","))
			{
				String trimmed = part.trim();
				if (trimmed.isEmpty())
				{
					continue;
				}
				order.add(canonicalByLower.getOrDefault(trimmed.toLowerCase(), trimmed));
			}
		}

		order.addAll(CATEGORY_ORDER);
		for (BossData data : filtered)
		{
			order.add(categoryOf(data));
		}

		return new ArrayList<>(order);
	}

	private void sortBosses(List<BossData> bosses, String category)
	{
		switch (config.sortMode())
		{
			case ALPHABETICAL:
				bosses.sort(Comparator.comparing(BossData::getDisplayName, String.CASE_INSENSITIVE_ORDER));
				break;
			case COMBAT_LEVEL:
				bosses.sort(Comparator
					.comparing((BossData b) -> b.getCombatLevel() == null ? Integer.MAX_VALUE : b.getCombatLevel())
					.thenComparing(BossData::getDisplayName, String.CASE_INSENSITIVE_ORDER));
				break;
			case DEFAULT:
			default:
				if (ALPHABETICAL_CATEGORIES.contains(category))
				{
					bosses.sort(Comparator.comparing(BossData::getDisplayName, String.CASE_INSENSITIVE_ORDER));
				}
				break;
		}
	}

	private void addCategorySection(String category, List<BossData> bosses)
	{
		if (bosses.isEmpty())
		{
			return;
		}

		listModel.addElement(category);
		if (isCollapsed(category))
		{
			return;
		}
		for (BossData data : bosses)
		{
			listModel.addElement(data);
		}
	}

	private boolean isCollapsed(String category)
	{
		String stored = configStore.get(collapseKey(category));
		if (stored != null)
		{
			return Boolean.parseBoolean(stored);
		}
		return config.collapseCategoriesByDefault();
	}

	private void toggleCollapsed(String category)
	{
		boolean nowCollapsed = !isCollapsed(category);
		configStore.set(collapseKey(category), Boolean.toString(nowCollapsed));

		int scrollValue = listScroll.getVerticalScrollBar().getValue();
		populateList(lastQuery);
		SwingUtilities.invokeLater(() -> listScroll.getVerticalScrollBar().setValue(scrollValue));
	}

	private static String collapseKey(String category)
	{
		return "collapsed_" + category;
	}

	/** Called by the plugin when the RuneLite config panel is used to change settings. */
	void onConfigChanged()
	{
		if (lastShownEntry != null && !cards.getComponent(0).isVisible())
		{
			showEntry(lastShownEntry);
		}
		populateList(lastQuery);
	}

	// ---- style helpers (read live from config so changes apply without restart) ----

	private Color accentColor()
	{
		return config.accentColor();
	}

	private Color itemNameColor()
	{
		return config.itemNameColor();
	}

	private Color rateColorBase()
	{
		return config.rateColor();
	}

	private Font categoryFont()
	{
		return config.fontChoice().derive(Font.BOLD, 12f);
	}

	private Font itemFont()
	{
		return config.fontChoice().derive(Font.PLAIN, 12f);
	}

	private Font titleFont()
	{
		return config.fontChoice().derive(Font.BOLD, 16f);
	}

	private Font cardTitleFont(boolean small)
	{
		return config.fontChoice().derive(Font.BOLD, small ? 9f : 12f);
	}

	// ---- collection tracking ----

	private String currentBossId;

	private boolean isCollected(String bossId, String item)
	{
		String stored = configStore.get(collectedKey(bossId, item));
		return Boolean.parseBoolean(stored);
	}

	private void setCollected(String bossId, String item, boolean collected)
	{
		if (collected)
		{
			configStore.set(collectedKey(bossId, item), "true");
		}
		else
		{
			configStore.unset(collectedKey(bossId, item));
		}
	}

	private static String collectedKey(String bossId, String item)
	{
		return "collected_" + bossId + "_" + item.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
	}

	/**
	 * Wraps a rate row with a collection-tracking checkbox when that feature is enabled;
	 * otherwise returns the plain row. The checkbox toggles its own row in place rather than
	 * re-rendering the whole page, so interactive cards (e.g. CoX's points calculator) aren't reset.
	 */
	private JComponent dropRow(String item, String rateText, Color rateColor, boolean bold)
	{
		return dropRow(item, rateText, rateColor, bold, currentCard);
	}

	private JComponent dropRow(String item, String rateText, Color rateColor, boolean bold, JPanel parentCard)
	{
		if (!config.enableCollectionTracking() || currentBossId == null)
		{
			return rateRow(item, rateText, rateColor, bold);
		}

		String bossId = currentBossId;

		JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
		row.setOpaque(false);
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		row.setBorder(new EmptyBorder(0, 0, 0, 0));

		boolean collected = isCollected(bossId, item);
		JCheckBox checkbox = new JCheckBox();
		checkbox.setOpaque(false);
		checkbox.setAlignmentX(Component.LEFT_ALIGNMENT);
		checkbox.setBorder(new EmptyBorder(0, 0, 0, 2));
		checkbox.setSelected(collected);

		row.add(checkbox);
		row.add(rateRow(item, rateText, collected ? config.collectedColor() : rateColor, bold, collected));

		checkbox.addActionListener(e ->
		{
			boolean nowCollected = checkbox.isSelected();
			setCollected(bossId, item, nowCollected);
			row.remove(1);
			row.add(rateRow(item, rateText, nowCollected ? config.collectedColor() : rateColor, bold, nowCollected));
			row.revalidate();
			row.repaint();
			if (parentCard != null)
			{
				fitCardHeight(parentCard);
			}
		});

		return row;
	}

	void showEntry(BossData data)
	{
		detailPanel.removeAll();
		currentBossId = data.getId();
		lastShownEntry = data;

		addTitle(data.getDisplayName());
		addImageIfPresent(data);

		switch (data.getType())
		{
			case SIMPLE:
			case MULTI_ROLL:
				if (data.getDropGroups() != null)
				{
					for (DropGroup group : data.getDropGroups())
					{
						beginCard(group.getGroupName());
						renderDropRows(group.getDrops(), false);
						renderCombinedRows(group.getCombined());
						endCard();
					}
				}
				else
				{
					beginCard("Drops");
					renderDropRows(data.getDrops(), false);
					renderCombinedRows(data.getCombined());
					endCard();
				}
				if (data.getPet() != null)
				{
					beginCard("Pet");
					renderDropRows(List.of(data.getPet()), true);
					endCard();
				}
				if (data.getSecondarySources() != null)
				{
					for (SecondarySource source : data.getSecondarySources())
					{
						beginCard(source.getSourceName(), true);
						renderDropRows(source.getDrops(), false);
						renderCombinedRows(source.getCombined());
						endCard();
					}
				}
				break;
			case KILLCOUNT_FORMULA:
				renderKillcountInteractive(data);
				beginCard("Items by brother");
				renderBrotherItems(data.getBrotherItems());
				endCard();
				break;
			case POINTS_BASED:
				renderPointsBasedInteractive(data);
				break;
			default:
				break;
		}

		if (data.getWikiUrl() != null)
		{
			addVerticalGap(6);
			JButton wikiButton = new JButton("View on Wiki");
			wikiButton.setAlignmentX(Component.LEFT_ALIGNMENT);
			wikiButton.setFocusPainted(false);
			wikiButton.addActionListener(e -> LinkBrowser.browse(data.getWikiUrl()));
			detailPanel.add(wikiButton);
		}

		refreshDetailLayout();
		cardLayout.show(cards, "detail");
	}

	private void refreshDetailLayout()
	{
		detailPanel.revalidate();
		detailPanel.repaint();
	}

	// ---- card / section helpers ----

	private JPanel currentCard;

	private void beginCard(String title)
	{
		beginCard(title, false);
	}

	private void beginCard(String title, boolean smallTitle)
	{
		JPanel card = new JPanel();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(new CompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.BORDER_COLOR),
			new EmptyBorder(6, 8, 6, 8)));
		card.setAlignmentX(Component.LEFT_ALIGNMENT);

		if (title != null)
		{
			JLabel titleLabel = new JLabel(wrap(title));
			titleLabel.setFont(cardTitleFont(smallTitle));
			titleLabel.setForeground(accentColor());
			titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
			titleLabel.setBorder(new EmptyBorder(0, 0, 4, 0));
			card.add(titleLabel);
		}

		currentCard = card;
	}

	private void endCard()
	{
		fitCardHeight(currentCard);
		detailPanel.add(currentCard);
		addVerticalGap(6);
		currentCard = null;
	}

	/**
	 * Caps a card's max height to its current content height. Must be called
	 * again any time a card's content changes size AFTER endCard() already ran
	 * (e.g. a spinner listener repopulating it) - otherwise the card stays
	 * frozen at its original (often much smaller) height and clips new content.
	 */
	private void fitCardHeight(JPanel card)
	{
		card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
	}

	// A small set of muted, low-contrast colors used to group drops that share
	// the same rate (e.g. Bandos chestplate/tassets/boots all being 1/381).
	private static final Color[] GROUP_COLORS = {
		new Color(0x6E, 0x95, 0xB7), // muted blue
		new Color(0x7F, 0xB3, 0x7F), // muted green
		new Color(0xA9, 0x8F, 0xC7), // muted lavender
		new Color(0x6F, 0xBF, 0xBF), // muted teal
		new Color(0xC9, 0x8F, 0x9E), // muted rose
		new Color(0xC6, 0xA5, 0x74), // muted amber
	};

	private void renderDropRows(List<Drop> drops, boolean emphasize)
	{
		if (drops == null)
		{
			return;
		}

		if (emphasize)
		{
			for (Drop drop : drops)
			{
				currentCard.add(dropRow(drop.getItem(), "1/" + NUMBER_FORMAT.format(drop.getDenominator()), accentColor(), true));
			}
			return;
		}

		if (!config.colorCodeSharedRates())
		{
			for (Drop drop : drops)
			{
				currentCard.add(dropRow(drop.getItem(), "1/" + NUMBER_FORMAT.format(drop.getDenominator()), rateColorBase(), false));
			}
			return;
		}

		java.util.Map<Integer, Color> colorByDenominator = new java.util.LinkedHashMap<>();
		for (Drop drop : drops)
		{
			colorByDenominator.computeIfAbsent(drop.getDenominator(),
				k -> GROUP_COLORS[colorByDenominator.size() % GROUP_COLORS.length]);
		}

		for (Drop drop : drops)
		{
			currentCard.add(dropRow(drop.getItem(), "1/" + NUMBER_FORMAT.format(drop.getDenominator()), colorByDenominator.get(drop.getDenominator()), false));
		}
	}

	private void renderCombinedRows(List<CombinedRate> combined)
	{
		if (combined == null)
		{
			return;
		}

		for (CombinedRate rate : combined)
		{
			currentCard.add(rateRow(rate.getLabel(), "1/" + NUMBER_FORMAT.format(rate.getDenominator()), true));
		}
	}

	private void renderBrotherItems(List<BrotherItems> brothers)
	{
		if (brothers == null)
		{
			return;
		}

		for (BrotherItems brother : brothers)
		{
			JLabel nameLabel = new JLabel(brother.getBrother());
			nameLabel.setForeground(itemNameColor());
			nameLabel.setFont(config.fontChoice().derive(Font.BOLD, 12f));
			nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
			currentCard.add(nameLabel);

			JLabel itemsLabel = new JLabel(wrap(String.join(", ", brother.getItems())));
			itemsLabel.setForeground(rateColorBase());
			itemsLabel.setFont(itemFont());
			itemsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
			itemsLabel.setBorder(new EmptyBorder(0, 0, 6, 0));
			currentCard.add(itemsLabel);
		}
	}

	/**
	 * Barrows: the stored killcountRates are the PER-ROLL denominator only.
	 * Rolls scale as baseRolls + extraRollsPerCount * count, so the real
	 * "any unique" and "specific item" odds are computed here, not stored.
	 */
	private void renderKillcountInteractive(BossData data)
	{
		int baseRolls = data.getBaseRolls() != null ? data.getBaseRolls() : 1;
		int extraRollsPerCount = data.getExtraRollsPerCount() != null ? data.getExtraRollsPerCount() : 0;
		int itemsPerCount = data.getItemsPerCount() != null ? data.getItemsPerCount() : 1;

		beginCard(data.getFormulaLabel());
		for (KillcountRate rate : data.getKillcountRates())
		{
			int rolls = baseRolls + extraRollsPerCount * rate.getCount();
			double perRoll = 1.0 / rate.getDenominator();
			double effective = 1 - Math.pow(1 - perRoll, rolls);
			double specific = effective / (itemsPerCount * rate.getCount());

			currentCard.add(rateRow(rate.getCount() + " killed - any unique", "1/" + NUMBER_FORMAT.format(Math.round(1 / effective)), true));
			currentCard.add(rateRow("   specific piece", "~1/" + NUMBER_FORMAT.format(Math.round(1 / specific)), false));
		}
		endCard();
	}

	/**
	 * Chambers of Xeric: bidirectional points/percent spinners (editing either
	 * updates the other), plus a Normal/Challenge Mode toggle for which
	 * specific unique you'd get if a roll succeeds.
	 */
	private void renderPointsBasedInteractive(BossData data)
	{
		final boolean[] updating = {false};

		beginCard("Points calculator");
		JSpinner pointsSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 5_000_000, (int) (double) data.getPointsPerPercent()));
		currentCard.add(controlRow("Team points:", pointsSpinner));
		currentCard.add(Box.createVerticalStrut(4));

		double capPercentValue = data.getCapPercent();
		JSpinner percentSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, capPercentValue, 1.0));
		currentCard.add(controlRow("Next roll %:", percentSpinner));
		endCard();

		beginCard("Result");
		JPanel resultCard = currentCard;
		JLabel resultLabel = new JLabel(wrap(calculateCoxChances(0, data)));
		resultLabel.setForeground(accentColor());
		resultLabel.setFont(itemFont());
		resultLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		currentCard.add(resultLabel);
		endCard();

		JComboBox<String> modeSelect = new JComboBox<>(new String[]{"Normal Mode", "Challenge Mode"});
		modeSelect.setMaximumSize(new Dimension(TEXT_WIDTH_PX, 24));
		modeSelect.setAlignmentX(Component.LEFT_ALIGNMENT);

		beginCard("If a unique hits, which one");
		JPanel uniqueCard = currentCard;
		currentCard.add(modeSelect);
		currentCard.add(Box.createVerticalStrut(6));

		JPanel weightsPanel = new JPanel();
		weightsPanel.setLayout(new BoxLayout(weightsPanel, BoxLayout.Y_AXIS));
		weightsPanel.setOpaque(false);
		weightsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		currentCard.add(weightsPanel);
		endCard();

		Runnable updateWeights = () ->
		{
			boolean challengeMode = modeSelect.getSelectedIndex() == 1;
			weightsPanel.removeAll();
			List<WeightedItem> items = challengeMode ? data.getUniqueItemsChallengeMode() : data.getUniqueItems();
			Integer total = challengeMode ? data.getUniqueItemsChallengeModeWeightTotal() : data.getUniqueItemsWeightTotal();
			if (items != null && total != null)
			{
				java.util.Map<Integer, Color> colorByWeight = new java.util.LinkedHashMap<>();
				for (WeightedItem item : items)
				{
					colorByWeight.computeIfAbsent(item.getWeight(),
						k -> GROUP_COLORS[colorByWeight.size() % GROUP_COLORS.length]);
				}

				for (WeightedItem item : items)
				{
					double pct = 100.0 * item.getWeight() / total;
					String oddsText = String.format("%.1f%% (1 in %s)", pct, formatOdds(total, item.getWeight()));
					weightsPanel.add(dropRow(item.getItem(), oddsText, colorByWeight.get(item.getWeight()), false, uniqueCard));
				}
			}
			fitCardHeight(uniqueCard);
			refreshDetailLayout();
		};
		modeSelect.addActionListener(e -> updateWeights.run());
		updateWeights.run();

		pointsSpinner.addChangeListener(e ->
		{
			if (updating[0])
			{
				return;
			}
			updating[0] = true;
			int points = (Integer) pointsSpinner.getValue();
			double nextRollPercent = Math.min(data.getCapPercent(), (points % data.getCapPoints()) / (double) data.getPointsPerPercent());
			if (points >= data.getCapPoints() && points % data.getCapPoints() == 0)
			{
				nextRollPercent = data.getCapPercent();
			}
			percentSpinner.setValue(nextRollPercent);
			resultLabel.setText(wrap(calculateCoxChances(points, data)));
			fitCardHeight(resultCard);
			updating[0] = false;
			refreshDetailLayout();
		});

		percentSpinner.addChangeListener(e ->
		{
			if (updating[0])
			{
				return;
			}
			updating[0] = true;
			double percent = (Double) percentSpinner.getValue();
			int points = (int) Math.round(percent * data.getPointsPerPercent());
			pointsSpinner.setValue(points);
			resultLabel.setText(wrap(calculateCoxChances(points, data)));
			fitCardHeight(resultCard);
			updating[0] = false;
			refreshDetailLayout();
		});

		if (data.getDrops() != null)
		{
			beginCard("Tertiary drop table");
			renderDropRows(data.getDrops(), false);
			renderCombinedRows(data.getCombined());
			endCard();
		}

		if (data.getPet() != null)
		{
			beginCard("Pet");
			renderDropRows(List.of(data.getPet()), true);
			endCard();
		}
	}

	private String calculateCoxChances(int points, BossData data)
	{
		int capPoints = data.getCapPoints();
		double pointsPerPercent = data.getPointsPerPercent();
		double capPercent = data.getCapPercent();
		int maxUniques = data.getMaxUniques();

		int fullRolls = Math.min(points / capPoints, maxUniques);
		int remainder = points - fullRolls * capPoints;
		double nextRollPercent = Math.min(capPercent, remainder / pointsPerPercent);

		StringBuilder sb = new StringBuilder();
		for (int i = 1; i <= fullRolls; i++)
		{
			sb.append("Roll ").append(i).append(": ").append(String.format("%.1f%%", capPercent)).append(" (capped)\n");
		}
		if (fullRolls < maxUniques)
		{
			sb.append("Roll ").append(fullRolls + 1).append(": ").append(String.format("%.1f%%", nextRollPercent));
		}
		else
		{
			sb.append("Maximum of ").append(maxUniques).append(" uniques reached.");
		}

		return sb.toString();
	}

	// ---- low-level row builders ----

	private JLabel rateRow(String name, String rateText, boolean emphasize)
	{
		return rateRow(name, rateText, emphasize ? accentColor() : rateColorBase(), emphasize);
	}

	private JLabel rateRow(String name, String rateText, Color rateColor, boolean bold)
	{
		return rateRow(name, rateText, rateColor, bold, false);
	}

	private JLabel rateRow(String name, String rateText, Color rateColor, boolean bold, boolean strikethrough)
	{
		String nameColorHex = toHex(itemNameColor());
		String rateColorHex = toHex(rateColor);
		String rateOpen = bold ? "<b>" : "";
		String rateClose = bold ? "</b>" : "";
		String nameHtml = escapeHtml(name);
		if (strikethrough)
		{
			nameHtml = "<s>" + nameHtml + "</s>";
		}

		String html = "<html><body style='width:" + TEXT_WIDTH_PX + "px'>"
			+ "<span style='color:" + nameColorHex + "'>" + nameHtml + ":</span>"
			+ "&nbsp;&nbsp;" + rateOpen + "<span style='color:" + rateColorHex + "'>" + escapeHtml(rateText) + "</span>" + rateClose
			+ "</body></html>";

		JLabel label = new JLabel(html);
		label.setFont(itemFont());
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		label.setBorder(new EmptyBorder(2, 0, 2, 0));
		return label;
	}

	private static String toHex(Color color)
	{
		return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
	}

	/** "1 in X" odds among successful unique rolls, given this item's share of the total weight. */
	private static String formatOdds(int total, int weight)
	{
		double odds = (double) total / weight;
		if (odds == Math.rint(odds))
		{
			return NUMBER_FORMAT.format((long) odds);
		}
		return String.format("%.1f", odds);
	}

	private JPanel controlRow(String labelText, Component control)
	{
		JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
		row.setOpaque(false);
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

		JLabel label = new JLabel(labelText);
		label.setForeground(itemNameColor());
		label.setFont(itemFont());
		label.setAlignmentX(Component.LEFT_ALIGNMENT);

		if (control instanceof JComponent)
		{
			((JComponent) control).setAlignmentX(Component.LEFT_ALIGNMENT);
		}
		control.setMaximumSize(new Dimension(TEXT_WIDTH_PX, 24));

		row.add(label);
		row.add(Box.createVerticalStrut(2));
		row.add(control);
		return row;
	}

	private void addImageIfPresent(BossData data)
	{
		java.awt.image.BufferedImage image = loadImage(data.getId());
		if (image == null)
		{
			return;
		}

		int maxWidth = TEXT_WIDTH_PX + 30;
		int width = Math.min(image.getWidth(), maxWidth);
		int height = (int) Math.round(image.getHeight() * (width / (double) image.getWidth()));
		java.awt.Image scaled = image.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH);

		JLabel imageLabel = new JLabel(new javax.swing.ImageIcon(scaled));
		imageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		detailPanel.add(imageLabel);
		addVerticalGap(8);
	}

	private java.awt.image.BufferedImage loadImage(String id)
	{
		try (java.io.InputStream is = getClass().getResourceAsStream("images/" + id + ".png"))
		{
			if (is == null)
			{
				return null;
			}
			return javax.imageio.ImageIO.read(is);
		}
		catch (java.io.IOException e)
		{
			return null;
		}
	}

	private void addTitle(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(titleFont());
		label.setForeground(accentColor());
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		detailPanel.add(label);

		JPanel underline = new JPanel();
		underline.setBackground(accentColor());
		underline.setAlignmentX(Component.LEFT_ALIGNMENT);
		underline.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
		underline.setPreferredSize(new Dimension(Integer.MAX_VALUE, 2));
		detailPanel.add(underline);

		addVerticalGap(8);
	}

	private void addVerticalGap(int px)
	{
		detailPanel.add(Box.createVerticalStrut(px));
	}

	private static String wrap(String text)
	{
		return "<html><body style='width:" + TEXT_WIDTH_PX + "px'>" + escapeHtml(text).replace("\n", "<br>") + "</body></html>";
	}

	private static String escapeHtml(String text)
	{
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	@FunctionalInterface
	private interface SimpleDocListener extends javax.swing.event.DocumentListener
	{
		void update();

		@Override
		default void insertUpdate(javax.swing.event.DocumentEvent e)
		{
			update();
		}

		@Override
		default void removeUpdate(javax.swing.event.DocumentEvent e)
		{
			update();
		}

		@Override
		default void changedUpdate(javax.swing.event.DocumentEvent e)
		{
			update();
		}
	}
}
