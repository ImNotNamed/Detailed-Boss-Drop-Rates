package com.customplugins.droprates;

import java.util.HashMap;
import java.util.Map;

/** In-memory ConfigStore for the offline screenshot harness. */
class FakeConfigStore implements ConfigStore
{
	private final Map<String, String> values = new HashMap<>();

	@Override
	public String get(String key)
	{
		return values.get(key);
	}

	@Override
	public void set(String key, String value)
	{
		values.put(key, value);
	}

	@Override
	public void unset(String key)
	{
		values.remove(key);
	}
}
