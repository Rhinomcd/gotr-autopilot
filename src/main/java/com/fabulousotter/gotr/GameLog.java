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
import com.fabulousotter.gotr.state.Snapshot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Turns each tick's snapshot and instruction into log lines for what changed since the last
 * tick, so a lost game can be read back as a timeline.
 */
class GameLog
{
	private Map<String, String> last = new LinkedHashMap<>();

	List<String> changes(Snapshot s, Instruction i, int greatGuardianHealth)
	{
		Map<String, String> now = watched(s, i, greatGuardianHealth);
		List<String> lines = new ArrayList<>();
		now.forEach((key, value) ->
		{
			if (!Objects.equals(last.get(key), value))
			{
				lines.add(key + ": " + value);
			}
		});
		if (!Objects.equals(last.get("step"), now.get("step")))
		{
			lines.add("  carrying: " + carrying(s));
		}
		last = now;
		return lines;
	}

	void reset()
	{
		last = new LinkedHashMap<>();
	}

	private static Map<String, String> watched(Snapshot s, Instruction i, int greatGuardianHealth)
	{
		Map<String, String> m = new LinkedHashMap<>();
		m.put("phase", s.getPhase() + " " + s.getLocation());
		m.put("step", i.getStep() + " \"" + i.getHeadline() + "\"" + (i.getDetail().isEmpty() ? "" : " (" + i.getDetail() + ")"));
		m.put("power", bucket(s.powerPercent(), 5) + "%");
		m.put("great guardian health", greatGuardianHealth < 0 ? "unseen" : bucket(greatGuardianHealth, 10) + "%");
		m.put("guardians", s.getGuardiansActive() + "/" + s.getGuardiansMax());
		m.put("barriers", barriers(s));
		m.put("broken tiles", String.valueOf(s.getBrokenTiles().size()));
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
			String health = b.getHealthPercent() < 0 ? "?" : bucket(b.getHealthPercent(), 10) + "%";
			j.add(b.getTier() + " " + health + " @" + b.getLocation().getX() + "," + b.getLocation().getY());
		}
		return j.toString();
	}

	private static String carrying(Snapshot s)
	{
		return "fragments " + s.getFragments()
			+ ", essence " + s.getEssence() + " + " + s.pouchStored() + " pouched"
			+ ", stones " + s.getElementalStones() + "e/" + s.getCatalyticStones() + "c/"
			+ s.getPolyElementalStones() + "pe/" + s.getPolyCatalyticStones() + "pc"
			+ ", uncharged cells " + s.getUnchargedCells()
			+ ", base rune " + s.getBaseRune() + " x" + s.getBaseRuneCount()
			+ ", necklace " + (s.isBindingNecklaceWorn() ? s.getNecklaceCharges() : "off")
			+ ", imbue " + s.isMagicImbueActive()
			+ ", chisel " + s.isChisel();
	}

	private static int bucket(int value, int size)
	{
		return value / size * size;
	}
}
