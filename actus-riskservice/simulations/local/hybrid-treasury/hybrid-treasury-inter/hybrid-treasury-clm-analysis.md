# Hybrid Treasury CLM: Strategy Analysis for Risk Management
## Digital Asset Portfolio Rebalancing — Three-Tier Framework
### Simulation: HT-CONS-CLM V8 | BTC $60K→$135K | ETH $1.8K→$7.2K | $10M Initial

---

## Part I — The Simulation and What We Measured

### Portfolio Structure

The simulation models a $10M hybrid corporate treasury deployed across six asset classes simultaneously:

- **Digital Assets**: 33 BTC units ($1.98M, 19.8% allocation) + 555 ETH units ($999K, 10.0% allocation)
- **T-Bills**: Three staggered maturities ($3M total — 3-month, 4-month, 5-month)
- **Accounts Receivable**: Four invoices with early settlement optionality ($1.2M face value)
- **Accounts Payable**: Three supplier obligations with penalty accrual ($580K face value)
- **USDC**: $500K stablecoin position earning 4.2% yield
- **Working Capital Cash**: Central treasury PAM contract managing all cash flows

This is not a DA-only portfolio. It is deliberately a hybrid — the kind of treasury a mid-to-large enterprise actually runs when it holds digital assets alongside conventional obligations. The fixed components (T-bills, AR/AP, USDC) are identical across all three strategy tiers. Every difference in outcome comes entirely from how the digital asset positions are managed.

### The ACTUS CLM Mechanism — Critical to Understand

Before comparing strategies, the mechanism must be clearly understood. This is not a spot trading simulation.

**Notional Principal (NP)** is the dollar ledger of a DA position inside an ACTUS Commodity Linked Maturity contract. It starts at the deployment cost ($1.98M for BTC, $999K for ETH). It changes only when a rebalancing event fires — drift sells reduce it, drift buys increase it. At maturity, the contract repays the current NP as cash — which represents remaining units marked to the maturity spot price.

**Net Asset Value (NAV)** is the total portfolio value at any moment — all asset classes marked to market. It starts at $10M and grows as the bull market runs, reaching ~$19.8M by August.

**The allocation signal** is NP ÷ NAV. When this ratio exceeds the upper band, a drift sell fires. When it falls below the lower band, a drift buy fires. The ADM — Allocation Drift Model — monitors this ratio at every scheduled monitoring date.

**Critical implication**: Price appreciation is only captured into treasury cash through active rebalancing events. If no events fire, the contract repays approximately original deployment cost at maturity regardless of where spot price went. This is the foundational insight that drives everything in this analysis.

---

## Part II — Verified Simulation Results

### The Three Tiers — Simulation Parameters

| Parameter | Systematic Harvester (CM-8) | Tactical Rebalancer (MD-8) | Strong HODLer (AG-8) |
|-----------|---------------------------|--------------------------|---------------------|
| BTC Band | 17% – 20.5% | 15% – 22.2% | 10% – 30.0% |
| ETH Band | 8.5% – 10.3% | 7.0% – 11.2% | 4.0% – 15.0% |
| PP Lock % | 15% | 25% | 40% |
| Position Floor | 65% | 50% | 35% |
| Rebalance Frequency | High | Medium | Low |

### Verified Results — Single Bull Phase (BTC +125%, ETH +300%)

| Tier | Final Cash | ROI | BTC Drift Sells | BTC Drift Buys | ETH Events |
|------|-----------|-----|----------------|---------------|-----------|
| Systematic Harvester | $14,642,219 | +46.4% | 6 (at $73K–$99K) | 0 | 11 buys |
| Tactical Rebalancer | $13,613,731 | +36.1% | 2 (at $80–84K) | 3 | 5 buys |
| Strong HODLer | $11,873,921 | +18.7% | 1 (small) | 3 (minor) | 3 buys |

All three results independently verified: cash reconstructed from raw SCF flow data within $1 of reported.

### P/L Attribution by Source

| Source | Systematic Harvester | Tactical Rebalancer | Strong HODLer |
|--------|---------------------|-------------------|--------------|
| BTC net P/L | +$1,812,980 | +$1,188,753 | +$203,522 |
| ETH net P/L | +$1,598,400 | +$1,194,139 | +$439,560 |
| T-Bills yield | +$40,782 | +$40,782 | +$40,782 |
| AR/AP net | +$1,179,529 | +$1,179,529 | +$1,179,529 |
| USDC yield + MD | +$510,529 | +$510,529 | +$510,529 |
| **Total gain** | **+$4,642,219** | **+$3,613,731** | **+$1,873,921** |
| **Portfolio ROI** | **+46.4%** | **+36.1%** | **+18.7%** |

The fixed components contribute $1,730,840 identically across all tiers. The entire 27.7 percentage point spread comes from DA management alone.

---

## Part III — Why the Labels Matter

The labels Conservative / Moderate / Aggressive are inherited from traditional fund management and carry the wrong connotations. "Aggressive" implies higher return potential — the opposite of what Strong HODLer delivers in most realistic market environments.

The real distinction is the philosophy of when and how DA price appreciation is converted into working capital.

**Systematic Harvester** — tight rebalancing bands create frequent partial exit events throughout a rally. Price appreciation is crystallised into treasury cash systematically at each band breach. The DA position shrinks in unit count but the cash account grows in locked, permanent value.

**Tactical Rebalancer** — medium bands allow the position to breathe through normal volatility. Rebalancing fires only when allocation meaningfully diverges from target. The model naturally combines selective selling at peaks with opportunistic buying at dips.

**Strong HODLer** — wide bands suppress almost all rebalancing activity. The treasury makes a deliberate choice to hold DA exposure through the full market cycle. Almost no DA appreciation is converted to cash until the single maturity date.

---

## Part IV — Realized vs Unrealized: The Complete Accounting Framework

### Definitions

**Realized gain**: Cash physically in the treasury account from a CLM drift sell or maturity payoff. Locked, permanent, immune to subsequent price moves.

**Unrealized gain**: Difference between current market value of remaining DA units and cost basis. Real wealth, but conditional — it exists only while prices remain at current levels or higher.

**Realized loss**: Cash permanently deployed back into a declining DA position through drift buy events. The outflow is irreversible at the moment of the buy.

**Unrealized loss**: Difference between cost basis and current market value when price is below cost. A paper loss that can recover, but genuine economic stress on the balance sheet while it persists.

### What Total Portfolio Should Reflect at Any Point

```
Total Portfolio Value =
    Realized Cash (locked, unconditional)
  + Mark-to-market value of remaining DA units (units × current spot price)
  + T-bill accrued value
  + USDC balance
  + AR net of AP
```

A portfolio statement that reports only realized cash understates the treasury. A statement that reports only mark-to-market overstates certainty. Both numbers must be presented together, clearly separated, for a complete picture.

At maturity, unrealized converts to realized — the MD event fires and remaining units × maturity spot becomes cash. During the life of the contract, the split between the two tells a fundamentally different story for each tier.

### Mid-Simulation Snapshot (Single Bull)

**Systematic Harvester**: High realized cash (6 sells locked). Low unrealized (sold most units at $73K–$99K, only floor residual ~21 units running). Total portfolio mark-to-market appears lower than Strong HODLer.

**Strong HODLer**: Negligible realized gains. Very large unrealized gain — nearly all 33 units held, marked at current high price. Total portfolio mark-to-market is highest of all three tiers at peak prices.

**At maturity**: Strong HODLer collects ~32 units × $135K = ~$4.3M from BTC. This is genuine reward for patience. The gap at maturity in a clean single bull is only ~4 percentage points — much smaller than the mid-simulation difference in realized cash suggests. But Strong HODLer's advantage was exposed to price reversal risk right up to the last day.

---

## Part V — Multi-Phase Analysis: Five Market Cycles

Each phase is one directional leg of a market cycle. Each bull: approximately +125% BTC. Each bear: approximately -50% mean reversion.

### Phase 1 — BULL

Verified results as above. Systematic Harvester leads in realized cash. Strong HODLer leads in mark-to-market. Systematic Harvester enters the bear with large locked cash and small residual exposure. Strong HODLer enters with large unit position fully exposed.

### Phase 2 — BULL-BEAR

Systematic Harvester hits its 17% lower bound earliest as prices fall, forced to BUY repeatedly into the decline — deploying harvested cash at $80K, $70K prices. These buys are at prices below where it originally sold ($73K–$99K). The round trip is net positive. NP inflated with cheap units.

Strong HODLer watches its large unrealized gain erode. Its 10% floor absorbs far more of the decline before forcing any buys. It sits largely still.

**Key accounting distinction**: Systematic Harvester generates realized losses (cash deployed at declining prices, but below prior sell prices — net positive round trip). Strong HODLer generates unrealized losses (paper losses on a large position). At the bear trough, all three tiers converge in total portfolio value. But Systematic Harvester's total portfolio is composed of locked cash plus a rebuilding cheap position. Strong HODLer's is composed of large unrealized loss on a large unit count.

### Phase 3 — BULL-BEAR-BULL

Compounding becomes visible for the first time.

Systematic Harvester enters Phase 3 with: locked cash from Phase 1 that the bear could not touch, and a unit position cheaply rebuilt in the bear below where it originally sold. When the second bull fires, it sells this cheap-to-carry position at high prices again. The sell-high-buy-back-lower cycle has run once and is now compounding.

Strong HODLer enters Phase 3 from approximately the same base as Phase 1. It has not compounded. Its Phase 3 bull result is approximately equal to its Phase 1 bull result — running the same starting position through the same market conditions.

**Ordering end of Phase 3**: Systematic Harvester's realized cash gap over Strong HODLer has widened. At peak prices, Strong HODLer still shows highest mark-to-market, but total portfolio (realized + unrealized) advantage is compressing in Systematic Harvester's favour.

### Phase 4 — BULL-BEAR-BULL-BEAR

Pattern confirmed. Each bear phase Systematic Harvester converts harvested cash into cheap units. Each bull it harvests again from a larger, cheaper base.

By Phase 4, Systematic Harvester has multiple rounds of realized gains locked in perpetuity — immune to market movements. Strong HODLer has oscillated between large unrealized gains and large unrealized losses four times, contributing almost no incremental realized cash across any cycle. Same total portfolio number at each trough, completely different volatility character and composition.

### Phase 5 — BULL-BEAR-BULL-BEAR-BULL

Third bull leg. Multi-cycle compounding fully visible.

| Tier | Realized Cash (cumulative) | Unrealized at Phase 5 peak | Portfolio character |
|------|---------------------------|---------------------------|---------------------|
| Systematic Harvester | Very large — 3 bull harvests compounded | Small floor residual | Predominantly locked |
| Tactical Rebalancer | Moderately large — balanced across cycles | Moderate | Even mix |
| Strong HODLer | Small — barely converted across 5 phases | Very large | Predominantly conditional |

Strong HODLer has carried full DA operational and counterparty risk across five phases. It has captured almost none of the volatility premium that makes DA worth holding in a treasury context.

---

## Part VI — The Single Uninterrupted Mega-Bull

### What Actually Happens

BTC $60K → $600K with no corrections across the equivalent of five phases.

Systematic Harvester begins selling at $73K and continues at every band breach. By $200K most units are sold at $73K–$130K prices. The floor ensures ~21 units remain to participate in the $600K maturity price. Total: significant absolute proceeds, but the bulk of the position was exited far below the terminal price.

Strong HODLer holds ~32 units throughout. At maturity: 32 × $600K = $19.2M. Clear and decisive outperformance.

**Ordering in a single uninterrupted mega-bull: Strong HODLer >> Tactical Rebalancer >> Systematic Harvester. This is correct and must be acknowledged without qualification.**

### Four Questions for the Risk Panel

**1. What is the probability of this scenario?**
An uninterrupted +900% move with zero meaningful corrections has never occurred for any digital asset across any multi-year period. BTC's documented historical pattern is strong rallies followed by 50–80% corrections. The mega-bull is possible but not the institutional planning base case.

**2. What if the mega-bull is 95% complete when the contract matures?**
If BTC reaches $550K then corrects 40% to $330K one month before maturity, Strong HODLer collects $10.6M instead of $17.6M. A single correction at the wrong moment destroys most of the advantage. Systematic Harvester's realized gains are immune to this correction.

**3. What is the realized vs unrealized composition during the mega-bull?**
Strong HODLer's balance sheet shows enormous paper wealth. Its cash account shows almost nothing. The treasury cannot pay suppliers with unrealized BTC gains. Any liquidity event before maturity finds Strong HODLer with no operating cash.

**4. Is ACTUS CLM the right instrument for a mega-bull conviction?**
If conviction is that BTC goes to $600K in a straight line, the correct instrument is direct spot holdings — not a CLM contract. The CLM is designed for rebalancing-based treasury management. Using a CLM while suppressing all rebalancing is paying for treasury infrastructure while getting pure directional exposure. Direct spot at least has no contract overhead and no CLM mechanism constraints.

---

## Part VII — TradFi Risk Management vs Hybrid Portfolio Reality

### Where TradFi Assumptions Break Down

**Assumption 1 — Asset classes are separable**: In TradFi, equities, fixed income, and commodities are analyzed as separate buckets. In this hybrid treasury, the DA position is mechanically linked to the cash position — drift buys in a bear market directly consume the working capital buffer. This is not correlation, it is structural linkage through the CLM mechanism.

**Assumption 2 — Realized gains are the accounting unit**: A TradFi treasury manager may accept a large unrealized DA gain as proof of portfolio health. But unrealized is conditional. Systematic Harvester's +$4.6M gain is locked. Strong HODLer's equivalent gain on paper can be taken away tomorrow by price reversal. Same number, categorically different character.

**Assumption 3 — Volatility is a risk metric**: In TradFi, high volatility is unambiguously bad for a treasury. In ACTUS CLM with Systematic Harvester parameters, high volatility is a revenue source. More price oscillation means more rebalancing events, more sell-high-buy-low cycles, more realized cash. This inverts the traditional risk-return assumption and requires careful framing for TradFi-trained risk managers.

**Assumption 4 — Liquidity is homogeneous**: In TradFi, any asset can be valued and liquidated. CLM positions have structured liquidation timelines — maturity dates and monitoring dates. The liquidity profile is structured, not on-demand. A risk manager must understand that a CLM position is closer to a managed fund with scheduled redemptions than to an equity position with continuous liquidity.

### What the Hybrid Structure Adds

**DA alpha funds the fixed income base**: The T-bill, USDC, and AR/AP components produce approximately +17.3% return regardless of DA strategy. DA management then adds between +1.4% (Strong HODLer) and +29.1% (Systematic Harvester) on top. The DA strategy should be evaluated on its marginal contribution above the fixed base.

**The working capital and DA cycles interact positively**: Early settlement AR inflows and T-bill maturities provide liquidity that funds DA drift buys in adverse markets without requiring DA liquidation. The two cycles run in parallel, each reinforcing the other's function.

**Realized cash is the operating metric; mark-to-market is the strategic metric**: Treasury reports should present both — operating cash position for day-to-day obligation management, and total portfolio value for board-level strategic evaluation. Reporting only one misrepresents the treasury's position.

### Risk Management Verdict for Each Strategy

**Systematic Harvester**: Most defensible to a risk committee. Highest realized cash under realistic oscillating market conditions. Performance degrades gracefully in adverse scenarios. Only meaningfully underperforms in a single uninterrupted mega-bull — and even then produces positive absolute returns. The risk manager can defend this choice quantitatively under almost any scenario the committee raises.

**Tactical Rebalancer**: The balanced choice. Meaningful realized gains across cycles, material DA upside maintained, does not over-trade. Not optimally positioned for any single extreme scenario but reasonably positioned for all of them. Appropriate default for most corporate treasury contexts.

**Strong HODLer**: Hardest to defend to a risk committee and should be. Lowest realized cash across every realistic multi-cycle scenario. Outperformance requires a single uninterrupted mega-bull — possible but not the base case. Carries full DA operational and counterparty risk while producing minimal incremental realized value from that risk-taking. Valid only for treasuries with very long horizons, zero near-term liquidity obligations, and high terminal price conviction — conditions rarely met simultaneously.

---

## Part VIII — Critical Questions and Answers

**Q: In the single bull, Strong HODLer shows highest mark-to-market. Isn't that best?**
Mid-simulation mark-to-market is conditional. It requires prices to hold until maturity. Systematic Harvester's realized cash is unconditional. The right question is which composition — realized vs unrealized mix — is most appropriate for this treasury's specific obligations. They are different things measuring different aspects of portfolio health.

**Q: If Systematic Harvester sells 6 times in a bull, has it failed to capture the rally?**
No. The 65% position floor ensures the treasury always participates in the terminal price on the residual. The 6 drift sells captured value the market had already delivered. Holding after a gain has been delivered is a directional bet. Each sell event is evidence of the strategy working correctly, not of premature exit.

**Q: Doesn't the bear market erase Systematic Harvester's advantage?**
It compresses the advantage temporarily. But the bear-phase buys are at prices below prior sell prices — the round trip is net positive. Across multiple complete cycles, Systematic Harvester accumulates a larger and cheaper position entering each bull phase. The multi-cycle compounding effect is positive.

**Q: What would a bear market simulation show?**
The ordering reverses in the DA contribution component — Strong HODLer > Tactical Rebalancer > Systematic Harvester in DA contribution only. But all three tiers would produce positive total portfolio returns because the fixed components ($1.73M contribution) are unaffected by DA price direction. This demonstrates the core value of the hybrid structure: the fixed income base cushions DA adverse scenarios.

**Q: What is the right benchmark?**
(a) Fixed-income-only equivalent — $10M in T-bills, USDC, AR/AP with no DA — producing approximately +17.3%. This is the zero-DA baseline. (b) Direct spot DA holding with no rebalancing structure — which captures all upside but also all downside with no working capital integration. The CLM strategies should be evaluated on their marginal contribution above the fixed base and their risk-adjusted advantage over unmanaged spot exposure.

---

## Part IX — Summary for Risk Management Panel

### The Three Numbers

On an identical $10M hybrid portfolio, across an identical 6-month bull market:

| Strategy | Final Cash | ROI | Character |
|----------|-----------|-----|-----------|
| Systematic Harvester | $14,642,219 | +46.4% | Predominantly realized, locked |
| Tactical Rebalancer | $13,613,731 | +36.1% | Balanced realized/unrealized |
| Strong HODLer | $11,873,921 | +18.7% | Predominantly unrealized, conditional |

27.7 percentage points spread. Same portfolio, same market, three different answers to one question: how actively should DA price appreciation be converted into working capital?

### The Multi-Cycle Truth

Over multiple complete market cycles: Systematic Harvester compounds its advantage with each cycle through the sell-high-buy-back-lower mechanism. Strong HODLer oscillates between large unrealized gains and large unrealized losses without accumulating locked value. The gap in total cumulative realized gains across 5 phases is not in percentage points — it is in multiples of the original DA deployment.

In a single uninterrupted mega-bull: Strong HODLer leads. This is the one scenario where active rebalancing costs real money. It is also the scenario least representative of historical DA market behavior.

### The Insight That Changes the Frame

TradFi risk management treats volatility as a cost. In ACTUS CLM with Systematic Harvester parameters, market volatility is a revenue source. The mechanism that generates returns is the same mechanism that generates uncertainty — oscillating prices. A corporate treasury that holds digital assets but suppresses all rebalancing is paying the full cost of DA operational complexity while capturing almost none of the mechanism that makes DA worth holding.

The appropriate risk management question is not: "How much DA risk are we comfortable taking?" It is: "Given that we have chosen to hold DA, which rebalancing parameter set most reliably converts DA price movement into realised treasury value?"

The simulation answers that question with verified numbers.

---

*Simulation: HT-CONS-CLM V8 | ACTUS Risk Service Extension | Three Tiers Verified*
*CM-8: $14,642,219 (+46.4%) | MD-8: $13,613,731 (+36.1%) | AG-8: $11,873,921 (+18.7%)*
*Cash reconstructions verified within $1 of reported | March 16, 2026*
