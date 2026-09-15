# Unique Drop Rates

A RuneLite plugin that shows a monster's unique drop rates - individual odds and
combined ("any unique") odds - before you go fight it, instead of needing to alt-tab
to the wiki.

Right-click a monster you can see for an instant "View drop rates" option, or use
the sidebar panel to search for any monster by name.

## Data coverage (v1)

This is an initial version covering one example of each reward mechanism found in
the game, as a foundation to expand from:

- **Simple table**: General Graardor
- **Multiple rolls per kill**: Zulrah (2 rolls/kill)
- **Killcount-dependent formula**: Barrows (chance depends on brothers killed)
- **Points-based, not tied to a single kill**: Chambers of Xeric
- **Points converted to table rolls**: Wintertodt
- **Superior slayer monsters** (rates vary a lot per monster): Greater abyssal
  demon, Colossal Hydra, Nechryarch

Data is bundled locally (`src/main/resources/com/customplugins/droprates/drop-data.json`)
rather than fetched live from the wiki, sourced from the OSRS Wiki's own published
rates. Anything not yet in the dataset simply won't show a menu option or search
result yet - more monsters can be added by extending that JSON file.
