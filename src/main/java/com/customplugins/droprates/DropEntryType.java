package com.customplugins.droprates;

/**
 * The reward mechanism a dataset entry uses - these are structurally different
 * (a flat per-item rate table isn't accurate for a boss whose odds depend on
 * killcount, team points, etc.) so each type gets its own rendering in the panel.
 */
enum DropEntryType
{
	/** A flat list of item:rate pairs, optionally with precomputed combined odds. */
	SIMPLE,
	/** Same as SIMPLE, but the table is rolled more than once per kill (e.g. Zulrah, Grotesque Guardians). */
	MULTI_ROLL,
	/** Odds depend on how many of a set of related NPCs were killed before looting (Barrows). */
	KILLCOUNT_FORMULA,
	/** Odds depend on total points earned across an activity, not a single kill (raids). */
	POINTS_BASED
}
