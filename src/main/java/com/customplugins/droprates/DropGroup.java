package com.customplugins.droprates;

import java.util.List;
import lombok.Getter;

/**
 * A distinct sub-table within one monster's drops (e.g. Zulrah's main uniques
 * vs. its separate mutagen sub-table) - kept apart so combined odds aren't
 * miscalculated across mechanically unrelated tables.
 */
@Getter
class DropGroup
{
	private String groupName;
	private List<Drop> drops;
	private List<CombinedRate> combined;
}
