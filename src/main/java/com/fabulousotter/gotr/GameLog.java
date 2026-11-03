/*
 * Copyright (c) 2026, FabulousOtter
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.fabulousotter.gotr;

import com.fabulousotter.gotr.plan.Instruction;
import com.fabulousotter.gotr.state.BarrierState;
import com.fabulousotter.gotr.state.GamePhase;
import com.fabulousotter.gotr.state.Location;
import com.fabulousotter.gotr.state.Snapshot;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import javax.annotation.Nullable;
import net.runelite.api.coords.WorldPoint;

/**
 * Turns each tick's snapshot and instruction into log lines for what changed since the last
 * tick, so a lost game can be read back as a timeline.
 */
class GameLog
{
	private static final String UNSEEN = "unseen";

	private Map<String, String> last = new LinkedHashMap<>();
	private GamePhase lastPhase;
	// The HUD resets power to zero on a loss, so the outcome reports the last power seen in play.
	private int lastActivePower;

	/**
	 * @param greatGuardianHealth percent, or -1 while its health bar is not showing
	 * @param enemies abyssal creatures in view, or null away from the temple
	 */
	List<String> changes(Snapshot s, Instruction i, int greatGuardianHealth, @Nullable String enemies)
	{
		Map<String, String> now = watched(s, i, greatGuardianHealth, enemies);
		if (s.getLocation() != Location.TEMPLE)
		{
			// The temple's NPCs and tiles unload away from it: what was last seen still stands.
			for (String key : TEMPLE_ONLY)
			{
				now.put(key, last.getOrDefault(key, UNSEEN));
			}
		}
		List<String> lines = new ArrayList<>();
		boolean over = s.getPhase() == GamePhase.CLOSING || s.getPhase() == GamePhase.ENDED;
		if (lastPhase == GamePhase.ACTIVE && over)
		{
			boolean won = lastActivePower >= 100 || (s.getMaxPower() > 0 && s.getPower() >= s.getMaxPower());
			lines.add("outcome: " + (won ? "WON" : "LOST at " + lastActivePower + "% power"));
		}
		if (s.getPhase() == GamePhase.ACTIVE)
		{
			lastActivePower = s.powerPercent();
		}
		lastPhase = s.getPhase();
		now.forEach((key, value) ->
		{
			boolean countdownOnly = key.equals("step") && Objects.equals(withoutNumbers(last.get(key)), withoutNumbers(value));
			if (!countdownOnly && !Objects.equals(last.get(key), value))
			{
				lines.add(key + ": " + value);
			}
		});
		if (!Objects.equals(withoutNumbers(last.get("step")), withoutNumbers(now.get("step"))))
		{
			if (!i.getDetail().isEmpty())
			{
				lines.add("  detail: " + i.getDetail());
			}
			lines.add("  carrying: " + carrying(s));
			lines.add("  timers: " + timers(s));
		}
		last = now;
		return lines;
	}

	void reset()
	{
		last = new LinkedHashMap<>();
		lastPhase = null;
		lastActivePower = 0;
	}

	private static final List<String> TEMPLE_ONLY = ImmutableList.of("great guardian health", "barriers", "broken tiles", "empty tiles", "enemies");

	private Map<String, String> watched(Snapshot s, Instruction i, int greatGuardianHealth, @Nullable String enemies)
	{
		Map<String, String> m = new LinkedHashMap<>();
		m.put("phase", s.getPhase() + " " + s.getLocation());
		m.put("step", i.getStep() + " \"" + i.getHeadline() + "\"");
		m.put("power", s.powerPercent() + "%");
		m.put("great guardian health", greatGuardianHealth < 0 ? UNSEEN : greatGuardianHealth + "%");
		m.put("guardians", s.getGuardiansActive() + "/" + s.getGuardiansMax() + " " + s.getGuardianTiers());
		m.put("barriers", barriers(s));
		m.put("broken tiles", tiles(s.getBrokenTiles()));
		m.put("empty tiles", tiles(s.getEmptyTiles()));
		m.put("enemies", Objects.toString(enemies, UNSEEN));
		m.put("altars", s.getActiveElemental() + " / " + s.getActiveCatalytic());
		m.put("portal", s.isPortalOpen() ? "open " + Objects.toString(s.getPortalDirection(), "") : "closed");
		m.put("cell", String.valueOf(s.getChargedCell()));
		m.put("energy", "elemental " + bucket(s.getElementalEnergy(), 50) + " catalytic " + bucket(s.getCatalyticEnergy(), 50));
		m.put("pouch degraded", String.valueOf(s.anyPouchDegraded()));
		return m;
	}

	private static String barriers(Snapshot s)
	{
		StringJoiner j = new StringJoiner(", ", "[", "]");
		for (BarrierState b : s.getBarriers())
		{
			String health = b.getHealthPercent() < 0 ? "?" : b.getHealthPercent() + "%";
			j.add(b.getTier() + " " + health + " @" + at(b.getLocation()));
		}
		return j.toString();
	}

	private static String tiles(List<WorldPoint> tiles)
	{
		StringJoiner j = new StringJoiner(", ", "[", "]");
		tiles.forEach(tile -> j.add(at(tile)));
		return j.toString();
	}

	private static String at(WorldPoint p)
	{
		return p.getX() + "," + p.getY();
	}

	private static String timers(Snapshot s)
	{
		return "altars change in " + s.getAltarSecondsRemaining() + "s"
			+ ", portal " + (s.isPortalOpen() ? "closes in " + s.getPortalSecondsRemaining() + "s" : "opens in " + s.getSecondsToNextPortal() + "s")
			+ ", rift closes in " + s.getSecondsToClose() + "s";
	}

	private static String carrying(Snapshot s)
	{
		return "fragments " + s.getFragments()
			+ ", essence " + s.getEssence() + " + " + s.pouchStored() + " pouched"
			+ ", stones " + s.getElementalStones() + "e/" + s.getCatalyticStones() + "c/"
			+ s.getPolyElementalStones() + "pe/" + s.getPolyCatalyticStones() + "pc"
			+ ", uncharged cells " + s.getUnchargedCells()
			+ ", base runes " + s.getBaseRunes()
			+ ", necklace " + (s.isBindingNecklaceWorn() ? s.getNecklaceCharges() : "off")
			+ ", imbue " + s.isMagicImbueActive()
			+ ", chisel " + s.isChisel();
	}

	// Headlines such as "Altars open in 3s" count down; only a different instruction is a new step.
	@Nullable
	private static String withoutNumbers(@Nullable String step)
	{
		return step == null ? null : step.replaceAll("\\d+", "#");
	}

	private static int bucket(int value, int size)
	{
		return value / size * size;
	}
}
