package com.customplugins.droprates;

public enum SortMode
{
	DEFAULT("Default"),
	ALPHABETICAL("Alphabetical"),
	COMBAT_LEVEL("Combat level");

	private final String label;

	SortMode(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
