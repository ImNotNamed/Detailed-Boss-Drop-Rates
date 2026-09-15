package com.customplugins.droprates;

import java.util.List;
import lombok.Getter;

/**
 * A different monster that can drop the same uniques as a boss, but at a
 * different rate (e.g. GWD bodyguards dropping the boss's own armour pieces).
 */
@Getter
class SecondarySource
{
	private String sourceName;
	private List<Drop> drops;
	private List<CombinedRate> combined;
}
