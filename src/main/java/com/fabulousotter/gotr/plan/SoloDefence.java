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
package com.fabulousotter.gotr.plan;

import com.fabulousotter.gotr.model.CellTier;
import com.fabulousotter.gotr.state.BarrierState;
import com.fabulousotter.gotr.state.Snapshot;
import javax.annotation.Nullable;
import lombok.Value;

/**
 * What stands between the creatures and the Great Guardian in a solo game, read against the
 * wiki's solo guide: the creatures all queue at the one centre barrier, and the Great
 * Guardian's power sets how many come.
 */
@Value
class SoloDefence
{
	// Above this power the rift sends more than a weak setup holds.
	static final int SAFE_POWER_PERCENT = 30;

	@Nullable
	BarrierState centre;
	int overchargedGuardians;
	int strongGuardians;

	static SoloDefence of(Snapshot s, @Nullable BarrierState centre)
	{
		int overcharged = 0;
		int strong = 0;
		for (CellTier tier : s.getGuardianTiers())
		{
			overcharged += tier == CellTier.OVERCHARGED ? 1 : 0;
			strong += tier.isAtLeast(CellTier.STRONG) ? 1 : 0;
		}
		return new SoloDefence(centre, overcharged, strong);
	}

	private boolean centreAtLeast(CellTier tier)
	{
		return centre != null && centre.getTier().isAtLeast(tier);
	}

	/**
	 * A setup that holds the rift's opening onslaught: an overcharged guardian behind any
	 * barrier, or a strong one behind a medium barrier. Only then is a portal worth leaving for.
	 */
	boolean holdsTheRift()
	{
		return (overchargedGuardians >= 1 && centreAtLeast(CellTier.WEAK))
			|| (strongGuardians >= 1 && centreAtLeast(CellTier.MEDIUM))
			|| strongGuardians >= 2;
	}

	// Strong enough to let the power climb past SAFE_POWER_PERCENT.
	boolean holdsHighPower()
	{
		return overchargedGuardians >= 1 || strongGuardians >= 2;
	}
}
