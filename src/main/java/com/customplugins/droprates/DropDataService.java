package com.customplugins.droprates;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
class DropDataService
{
	private final List<BossData> entries = new ArrayList<>();
	private final Map<String, BossData> byNpcName = new HashMap<>();

	@Inject
	DropDataService(Gson gson)
	{
		try (InputStream is = getClass().getResourceAsStream("drop-data.json"))
		{
			if (is == null)
			{
				log.warn("drop-data.json not found on classpath");
			}
			else
			{
				DropDataRoot root = gson.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), DropDataRoot.class);
				if (root != null && root.getEntries() != null)
				{
					entries.addAll(root.getEntries());
				}
			}
		}
		catch (IOException e)
		{
			log.warn("Failed to load drop-data.json", e);
		}

		for (BossData entry : entries)
		{
			if (entry.getNpcNames() == null)
			{
				continue;
			}

			for (String npcName : entry.getNpcNames())
			{
				byNpcName.put(npcName.toLowerCase(), entry);
			}
		}
	}

	List<BossData> getAll()
	{
		return Collections.unmodifiableList(entries);
	}

	BossData findByNpcName(String npcName)
	{
		if (npcName == null)
		{
			return null;
		}

		return byNpcName.get(npcName.toLowerCase());
	}

	/**
	 * Matches by boss name OR by any item name the boss can drop (main drops,
	 * drop groups, pet, secondary sources, CoX-style weighted uniques), so
	 * searching "shard" finds every boss with a shard drop.
	 */
	List<BossData> search(String query)
	{
		String lower = query.toLowerCase().trim();
		if (lower.isEmpty())
		{
			return getAll();
		}

		List<BossData> results = new ArrayList<>();
		for (BossData entry : entries)
		{
			if (entry.getDisplayName().toLowerCase().contains(lower) || matchesItemName(entry, lower))
			{
				results.add(entry);
			}
		}

		return results;
	}

	private static boolean matchesItemName(BossData entry, String lower)
	{
		if (containsMatch(entry.getDrops(), Drop::getItem, lower))
		{
			return true;
		}

		if (entry.getDropGroups() != null)
		{
			for (DropGroup group : entry.getDropGroups())
			{
				if (containsMatch(group.getDrops(), Drop::getItem, lower))
				{
					return true;
				}
			}
		}

		if (entry.getPet() != null && entry.getPet().getItem().toLowerCase().contains(lower))
		{
			return true;
		}

		if (entry.getSecondarySources() != null)
		{
			for (SecondarySource source : entry.getSecondarySources())
			{
				if (containsMatch(source.getDrops(), Drop::getItem, lower))
				{
					return true;
				}
			}
		}

		if (containsMatch(entry.getUniqueItems(), WeightedItem::getItem, lower))
		{
			return true;
		}

		if (containsMatch(entry.getUniqueItemsChallengeMode(), WeightedItem::getItem, lower))
		{
			return true;
		}

		if (entry.getBrotherItems() != null)
		{
			for (BrotherItems brother : entry.getBrotherItems())
			{
				for (String item : brother.getItems())
				{
					if (item.toLowerCase().contains(lower))
					{
						return true;
					}
				}
			}
		}

		return false;
	}

	private static <T> boolean containsMatch(List<T> list, java.util.function.Function<T, String> nameExtractor, String lower)
	{
		if (list == null)
		{
			return false;
		}

		for (T item : list)
		{
			if (nameExtractor.apply(item).toLowerCase().contains(lower))
			{
				return true;
			}
		}

		return false;
	}
}
