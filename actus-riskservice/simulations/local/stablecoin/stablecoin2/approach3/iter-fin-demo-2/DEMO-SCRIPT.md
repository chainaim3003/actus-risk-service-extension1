# Converge.fi — Demo v2 Script
## MINT → HALT → RESTORE with Circulating Supply Awareness

---

## KEY IMPROVEMENT OVER v1

In v1, tokenSupply was a static $500K — it never changed across phases.
In v2, tokenSupply = **circulatingSupply + mintAskAmount**, reflecting reality:
- Phase A: 0 circulating + 100K ask = 100K to evaluate
- Phase B: 100K circulating + 100K ask = 200K to evaluate
- Phase C: 100K circulating + 100K ask = 200K to evaluate

This creates a **stronger narrative**: Phase B backing is 207% (PASSES a simple check),
but liquidity is 7.2% (FAILS). Only Converge.fi catches the real risk.

---

## NUMBERS CROSS-CHECK

| Metric | Phase A | Phase B | Phase C |
|--------|---------|---------|---------|
| Cash | $130,000 | $30,000 | $241,250 |
| T-bills | $385,000 | $385,000 | $260,000 |
| Total reserves | $515,000 | $415,000 | $501,250 |
| Circulating supply | 0 | 100,000 | 100,000 |
| Mint ask | 100,000 | 100,000 | 100,000 |
| **Supply to evaluate** | **100,000** | **200,000** | **200,000** |
| Backing % | 515.0% ✅ | 207.5% ✅ | 250.6% ✅ |
| Liquidity % | 25.2% ✅ | 7.2% ❌ | 48.1% ✅ |
| Risk score | 0 ✅ | 14 ✅ | 0 ✅ |
| Mint gate | OPEN | **CLOSED** | OPEN |
| **Blocked by** | — | **LIQUIDITY** (not backing!) | — |
| cvUSD minted | +100,000 | BLOCKED | +100,000 |
| cvUSD balance | 100,000 | 100,000 | 200,000 |
| Penalty cost | $0 | $0 | $3,750 |

---

## BEFORE DEMO — Clean Burn

```powershell
cd C:\SATHYA\CHAINAIM3003\mcp-servers\CRE\CRE11\converge.fi-1
npx hardhat console --network sepolia
> const s = await ethers.getContractAt("ConvergeStablecoin", "0x8D8131547Ec5Cb2fF1bB941a28fA20e347A928F3")
> const bal = await s.balanceOf("0x0c5e419D592d116bD9cE3DeE3D613F8b166e42EE")
> if (bal > 0n) await s.burn(bal)
> (await s.totalSupply()).toString()
> // Should be "0"
```

---

## Phase A: HEALTHY → Mint 100,000 cvUSD ✅

```powershell
# Browser check first:
# http://localhost:3001/api/demo/health-check

# Push healthy report on-chain
cd C:\SATHYA\CHAINAIM3003\mcp-servers\CRE\CRE11\converge.fi-1
$env:REPORT_MODE="demo-healthy-v2"; npx hardhat run scripts/push-report.ts --network sepolia

# Mint
npx hardhat console --network sepolia
> const s = await ethers.getContractAt("ConvergeStablecoin", "0x8D8131547Ec5Cb2fF1bB941a28fA20e347A928F3")
> const [w] = await ethers.getSigners()
> await s.mint(w.address, ethers.parseEther("100000"))
> // ✅ SUCCESS
```

Narrative: "Reserves are $515K. We're asking to mint 100K with 0 in circulation.
515% backed, 25.2% liquid. All gates pass."

---

## Phase B: STRESS → Mint 100,000 cvUSD 🔴 BLOCKED

```powershell
# Browser check:
# http://localhost:3001/api/demo/health-check?phase=B

# Push stressed report on-chain
$env:REPORT_MODE="demo-stressed-v2"; npx hardhat run scripts/push-report.ts --network sepolia

# Attempt mint — will FAIL
npx hardhat console --network sepolia
> const s = await ethers.getContractAt("ConvergeStablecoin", "0x8D8131547Ec5Cb2fF1bB941a28fA20e347A928F3")
> const [w] = await ethers.getSigners()
> await s.mint(w.address, ethers.parseEther("100000"))
> // 🔴 REVERTS: MintBlockedLiquidity(720, 1000)
```

Narrative: "Cash was drained by $100K. But here's the critical insight:
Backing is STILL 207% — a simple reserves-vs-supply check would APPROVE this mint.
Only Converge.fi catches that $385K of reserves are locked in T-bills.
Immediate cash is $30K — only 7.2% of reserves, below the 10% MiCA threshold.
Mint BLOCKED by the liquidity gate, not the backing gate."

---

## Phase C: RESTORE → Mint 100,000 cvUSD ✅

```powershell
# Browser check:
# http://localhost:3001/api/demo/health-check?phase=C

# Push restored report on-chain
$env:REPORT_MODE="demo-restored-v2"; npx hardhat run scripts/push-report.ts --network sepolia

# Mint
npx hardhat console --network sepolia
> const s = await ethers.getContractAt("ConvergeStablecoin", "0x8D8131547Ec5Cb2fF1bB941a28fA20e347A928F3")
> const [w] = await ethers.getSigners()
> await s.mint(w.address, ethers.parseEther("100000"))
> // ✅ SUCCESS
> (await s.balanceOf(w.address)).toString()
> // "200000000000000000000000" = 200,000 cvUSD
```

---

## REFERENCES

- ACTUS Taxonomy: https://www.actusfrf.org/taxonomy
- ACTUS PAM: https://documentation.actusfrf.org/docs/examples/basic-contract-types/example_PAM
- Chainlink CRE: https://docs.chain.link/cre
- MiCA Art.45: Immediate redemption at par (10% liquidity requirement)
- GENIUS Act: 1:1 reserve backing requirement
- OpenZeppelin ERC20: https://docs.openzeppelin.com/contracts/5.x/erc20
