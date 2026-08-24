# Compatibility audit

Audit date: 2026-08-24. “Natural preview compatibility” means the framework uses Minecraft's final player renderer chain and does not import or reset the target's model/animation internals. It is not a claim that the target's data API is integrated.

| Project | Audited 1.21.1 branch/tag/API | 0.1.0 status |
|---|---|---|
| NeoForged Documentation | `1.21.1` docs: `CustomPacketPayload`, `PayloadRegistrar`, data components | Implemented |
| NeoForge MDK | `MDK-1.21.1-ModDevGradle` main; upstream showed ModDevGradle 2.0.144 / NeoForge 21.1.235 | Project uses cached/tested ModDevGradle 2.0.137 / NeoForge 21.1.244, still 1.21.1/21.1.x |
| Curios | official API Maven pattern; verified 1.21.1 line `9.5.1+1.21.1`, dynamic `ICuriosItemHandler`/slot map model | Provider boundary only; no Curios artifact in test matrix |
| Beyond Dimensions | repository branch `1.21.1`, 0.3.0+; README confirms `DimensionsNet.getNetFromPlayer(Player)`, `getUnifiedStorage()`, `UnifiedStorage` and long counts | Not bound: exact bounded query/revision/extract/insert contracts were not stable/documented enough to safely implement without the artifact |
| Epic Fight | branch `1.21.1`, observed 21.17.x; official `yesman.epicfight.api` and Neo events | Natural preview compatibility; stat/weapon adapter not shipped |
| Epic Fight: Skill Tree | repository `epicskills`; observed 1.21.1 EpicSkills 21.2.4 | `SkillProvider`/tree model only; original-screen fallback adapter not shipped |
| Yes Steve Model | official repository warns public API/schema are not frozen; observed `2.6.5-neoforge mc1.21.1-release` | Natural preview compatibility; no private import/reflection |
| RarityCore | official repository 1.21.1 NeoForge instructions and Java API link | `RarityProvider` plus vanilla fallback; adapter not shipped because exact 1.21.1 API artifact was unavailable |
| Iron's Spells 'n Spellbooks | branch `1.21`; official `io.redspace.ironsspellbooks.api`, `SpellRegistry`, `SchoolRegistry`; observed 1.21.1-3.15.6 | `SpellProvider` model only; no copyrighted UI/assets copied |
| FTB Quests | branches `1.21.1/main` / `1.21.1/dev`, releases through `v2101.1.29` | Provider boundary only; reward mutation not attempted |
| Controlify | official repository supports 1.21.1 NeoForge and data-driven glyphs | Controller/glyph provider APIs only; keyboard/mouse complete |
| JourneyMap API | tag `1.21.1_2.0.0`, artifact `journeymap-api-neoforge:2.0.0-1.21.1`, official soft-dependency/no-shading guidance | Map/waypoint provider boundary only |
| minecraftPlayerAnimator | branch `1.21`, package versions through `2.0.4+1.21.1` | Natural preview compatibility |
| Better Combat | branch/release `2.4.0+1.21.1` | Natural preview compatibility; no attack/pose changes |

No third-party source, texture, font, icon, sound or GUI was copied. No mixins are present. No optional third-party class appears in common/core signatures, so an absent mod cannot trigger verifier classloading.

## Why the adapters are not falsely marked complete

The required safety bar includes exact 1.21.1 artifacts, dedicated-server classloading and mutation regression tests with the actual mods. Those binaries/test instances were not present in the workspace. The framework therefore leaves real provider interfaces and authoritative transaction hooks, but does not guess API signatures or substitute broad reflection. Beyond Dimensions in particular needs source-supported bounded enumeration and reversible extract/insert methods before its adapter can meet the no-duplication/no-loss contract.
