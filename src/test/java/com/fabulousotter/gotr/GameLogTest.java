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

import com.fabulousotter.gotr.model.CellTier;
import com.fabulousotter.gotr.plan.Instruction;
import com.fabulousotter.gotr.plan.Step;
import com.fabulousotter.gotr.state.BarrierState;
import com.fabulousotter.gotr.state.GamePhase;
import com.fabulousotter.gotr.state.Location;
import com.fabulousotter.gotr.state.Snapshot;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import net.runelite.api.coords.WorldPoint;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class GameLogTest
{
	private static final Instruction MINE = Instruction.builder().step(Step.MINE_FRAGMENTS).headline("Mine").build();

	private static Snapshot withBarrier(int health)
	{
		return Snapshot.builder()
			.location(Location.TEMPLE)
			.barriers(ImmutableList.of(new BarrierState(new WorldPoint(3610, 9497, 0), CellTier.STRONG, health)))
			.build();
	}

	@Test
	public void logsOnlyWhatChanged()
	{
		GameLog log = new GameLog();
		assertTrue(log.changes(withBarrier(90), MINE, -1, null).size() > 1);
		assertEquals(Collections.emptyList(), log.changes(withBarrier(90), MINE, -1, null));
		assertEquals(Collections.singletonList("barriers: [STRONG 44% @3610,9497]"), log.changes(withBarrier(44), MINE, -1, null));
	}

	@Test
	public void keepsTempleStateWhileInAnAltar()
	{
		GameLog log = new GameLog();
		log.changes(withBarrier(90), MINE, -1, null);
		Snapshot altar = withBarrier(90).toBuilder().location(Location.ALTAR_ROOM).barriers(ImmutableList.of()).build();
		assertEquals(Collections.singletonList("phase: WAITING ALTAR_ROOM"), log.changes(altar, MINE, -1, null));
	}

	@Test
	public void reportsALossAtThePowerLastSeenInPlay()
	{
		GameLog log = new GameLog();
		Snapshot active = withBarrier(90).toBuilder().phase(GamePhase.ACTIVE).power(70).maxPower(100).build();
		log.changes(active, MINE, -1, null);
		Snapshot ended = active.toBuilder().phase(GamePhase.ENDED).power(0).build();
		assertTrue(log.changes(ended, MINE, -1, null).contains("outcome: LOST at 70% power"));
	}
}
