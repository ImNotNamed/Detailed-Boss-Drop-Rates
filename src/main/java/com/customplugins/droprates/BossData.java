package com.customplugins.droprates;

import java.util.List;
import lombok.Getter;

@Getter
class BossData
{
	private String id;
	private String displayName;
	private String category;
	private Integer combatLevel;
	private List<String> npcNames;
	private DropEntryType type;
	private String wikiUrl;

	// MULTI_ROLL
	private Integer rollsPerKill;

	// SIMPLE / MULTI_ROLL
	private List<Drop> drops;
	private List<CombinedRate> combined;

	// SIMPLE / MULTI_ROLL - use instead of drops/combined when a monster has more
	// than one mechanically-separate table (e.g. Zulrah's uniques vs. mutagens)
	private List<DropGroup> dropGroups;

	// SIMPLE / MULTI_ROLL - a pet is often a separate roll with its own rules
	private Drop pet;

	// SIMPLE - other monsters that can drop the same uniques at a different rate
	private List<SecondarySource> secondarySources;

	// KILLCOUNT_FORMULA - killcountRates holds the PER-ROLL denominator for each count;
	// rolls(count) = baseRolls + extraRollsPerCount * count, and the effective
	// "any unique" / "specific item" chances are computed from that, not stored directly
	private String formulaLabel;
	private List<KillcountRate> killcountRates;
	private Integer baseRolls;
	private Integer extraRollsPerCount;
	private Integer itemsPerCount;
	private List<BrotherItems> brotherItems;

	// POINTS_BASED
	private Integer pointsPerPercent;
	private Double capPercent;
	private Integer capPoints;
	private Integer maxUniques;
	private List<WeightedItem> uniqueItems;
	private Integer uniqueItemsWeightTotal;
	private List<WeightedItem> uniqueItemsChallengeMode;
	private Integer uniqueItemsChallengeModeWeightTotal;

}
