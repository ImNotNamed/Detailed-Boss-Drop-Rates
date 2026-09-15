package com.customplugins.droprates;

/**
 * Thin wrapper around persisted per-key string state (collapsed categories, collected items).
 * Exists so the offline screenshot harness can supply a trivial in-memory fake instead of
 * needing a real RuneLite ConfigManager.
 */
interface ConfigStore
{
	String get(String key);

	void set(String key, String value);

	void unset(String key);
}
