# from fastapi import FastAPI
# from pydantic import BaseModel
# import torch
# from transformers import AutoTokenizer, AutoModelForSequenceClassification
# import re

# app = FastAPI()

# # ── ML Model ───────────────────────────────────────────────────────────────────
# MODEL_DIR  = "finetuned_codebert_512"
# MAX_LENGTH = 512
# THRESHOLD  = 0.3
# DEVICE     = torch.device("cuda" if torch.cuda.is_available() else "cpu")

# tokenizer = AutoTokenizer.from_pretrained(MODEL_DIR)
# model     = AutoModelForSequenceClassification.from_pretrained(MODEL_DIR)
# model.to(DEVICE)
# model.eval()

# # ── Rule-Based Detectors ───────────────────────────────────────────────────────
# def check_reentrancy(code):
#     """Detect call before state update pattern"""
#     # Find all call.value or low-level call patterns
#     call_pattern    = re.compile(r'\.call[\.\{].*?value.*?\(|\.call\(', re.DOTALL)
#     balance_pattern = re.compile(r'balances\[.*?\]\s*[-+]?=|balance\s*[-+]?=')

#     calls   = [(m.start(), m.group()) for m in call_pattern.finditer(code)]
#     updates = [(m.start(), m.group()) for m in balance_pattern.finditer(code)]

#     for call_pos, _ in calls:
#         for update_pos, _ in updates:
#             if call_pos < update_pos:
#                 return True, "Reentrancy: external call before state update"
#     return False, None

# def check_tx_origin(code):
#     """Detect tx.origin authentication"""
#     if re.search(r'tx\.origin', code):
#         return True, "tx.origin used for authentication"
#     return False, None

# def check_unchecked_send(code):
#     """Detect unchecked send/transfer return values"""
#     if re.search(r'\.send\((?!.*require)', code):
#         return True, "Unchecked send return value"
#     return False, None

# def check_integer_overflow(code):
#     """Detect potential overflow in old solidity"""
#     version = re.search(r'pragma solidity\s+\^?(\d+\.\d+)', code)
#     if version:
#         major, minor = version.group(1).split('.')
#         if int(major) == 0 and int(minor) < 8:
#             if re.search(r'\+=|-=|\*=', code) and not re.search(r'SafeMath', code):
#                 return True, "Potential integer overflow (Solidity < 0.8, no SafeMath)"
#     return False, None

# def check_selfdestruct(code):
#     """Detect selfdestruct usage"""
#     if re.search(r'selfdestruct|suicide', code):
#         return True, "selfdestruct present"
#     return False, None

# RULES = [
#     check_reentrancy,
#     check_tx_origin,
#     check_unchecked_send,
#     check_integer_overflow,
#     check_selfdestruct,
# ]

# # ── Request Schema ─────────────────────────────────────────────────────────────
# class ContractRequest(BaseModel):
#     source_code: str

# # ── Predict Endpoint ───────────────────────────────────────────────────────────
# @app.post("/predict")
# def predict(req: ContractRequest):
#     code = req.source_code

#     # 1. Run rule-based checks
#     rule_flags = []
#     for rule in RULES:
#         flagged, reason = rule(code)
#         if flagged:
#             rule_flags.append(reason)

#     # 2. Run ML model
#     inputs = tokenizer(
#         code, truncation=True, padding="max_length",
#         max_length=MAX_LENGTH, return_tensors="pt"
#     )
#     inputs = {k: v.to(DEVICE) for k, v in inputs.items()}
#     with torch.no_grad():
#         logits = model(**inputs).logits
#     prob = torch.softmax(logits, dim=1)[0][1].item()
#     ml_prediction = 1 if prob >= THRESHOLD else 0

#     # 3. Combine: vulnerable if ML OR any rule flags it
#     final_prediction = 1 if (ml_prediction == 1 or len(rule_flags) > 0) else 0

#     return {
#         "ml_probability":    round(prob, 4),
#         "ml_prediction":     ml_prediction,
#         "rule_flags":        rule_flags,
#         "final_prediction":  final_prediction,
#         "verdict":           "VULNERABLE" if final_prediction == 1 else "CLEAN"
#     }














from fastapi import FastAPI
from pydantic import BaseModel
import torch
from transformers import AutoTokenizer, AutoModelForSequenceClassification
import re

app = FastAPI()

# ── ML Model ───────────────────────────────────────────────────────────────────
MODEL_DIR  = "finetuned_codebert_512"
MAX_LENGTH = 512
THRESHOLD  = 0.3
DEVICE     = torch.device("cuda" if torch.cuda.is_available() else "cpu")

tokenizer = AutoTokenizer.from_pretrained(MODEL_DIR)
model     = AutoModelForSequenceClassification.from_pretrained(MODEL_DIR)
model.to(DEVICE)
model.eval()

# ── Rule-Based Detectors ───────────────────────────────────────────────────────
def check_reentrancy(code):
    """Detect call before state update pattern"""
    call_pattern    = re.compile(r'\.call[\.\{].*?value.*?\(|\.call\(', re.DOTALL)
    balance_pattern = re.compile(r'balances\[.*?\]\s*[-+]?=|balance\s*[-+]?=')

    calls   = [(m.start(), m.group()) for m in call_pattern.finditer(code)]
    updates = [(m.start(), m.group()) for m in balance_pattern.finditer(code)]

    for call_pos, _ in calls:
        for update_pos, _ in updates:
            if call_pos < update_pos:
                return True, "reentrancy", (
                    "Reentrancy vulnerability: your contract makes an external call "
                    "BEFORE updating its internal state (e.g. balances). An attacker "
                    "can call back into your function repeatedly before the balance "
                    "is deducted — this is exactly how the DAO hack stole $60M. "
                    "Fix: move all state updates ABOVE the external call."
                )
    return False, None, None


def check_tx_origin(code):
    """Detect tx.origin authentication"""
    if re.search(r'tx\.origin', code):
        return True, "tx_origin", (
            "Dangerous authentication method: tx.origin refers to the original "
            "wallet that started the transaction chain, not the immediate caller. "
            "A malicious contract can trick a legitimate user into calling it — "
            "tx.origin will still be the victim's address, bypassing your check. "
            "Fix: replace every tx.origin with msg.sender."
        )
    return False, None, None


def check_unchecked_send(code):
    """Detect unchecked send return values"""
    if re.search(r'\.send\((?!.*require)', code):
        return True, "unchecked_send", (
            "Unchecked .send() return value: unlike .transfer(), the .send() "
            "function does NOT revert on failure — it returns false silently. "
            "If you don't check the return value, your contract will continue "
            "running even if the ETH transfer failed, causing accounting errors. "
            "Fix: (bool success,) = addr.call{value: amount}(''); require(success, 'Failed');"
        )
    return False, None, None


def check_integer_overflow(code):
    """Detect potential overflow in old Solidity"""
    version = re.search(r'pragma solidity\s+\^?(\d+\.\d+)', code)
    if version:
        major, minor = version.group(1).split('.')
        if int(major) == 0 and int(minor) < 8:
            if re.search(r'\+=|-=|\*=', code) and not re.search(r'SafeMath', code):
                return True, "integer_overflow", (
                    "Integer overflow risk: Solidity versions below 0.8.0 do NOT "
                    "automatically check arithmetic operations. A uint8 variable "
                    "holding 255 silently wraps to 0 when you add 1. This was "
                    "exploited in the BECToken hack (2018) to generate infinite tokens. "
                    "Fix: upgrade to Solidity 0.8.x or import OpenZeppelin SafeMath."
                )
    return False, None, None


def check_selfdestruct(code):
    """Detect selfdestruct usage"""
    if re.search(r'selfdestruct|suicide', code):
        return True, "selfdestruct", (
            "selfdestruct detected: this opcode permanently destroys the contract "
            "and forcefully sends all ETH to a target address. If this function "
            "is not properly protected, anyone can destroy your contract and "
            "steal its entire ETH balance. "
            "Fix: add onlyOwner modifier and consider removing selfdestruct entirely."
        )
    return False, None, None


RULES = [
    check_reentrancy,
    check_tx_origin,
    check_unchecked_send,
    check_integer_overflow,
    check_selfdestruct,
]


# ── Confidence Classification ──────────────────────────────────────────────────
def classify_confidence(prob: float) -> tuple[str, str]:
    """Returns (level_label, plain_english_explanation)"""

    if prob < 0.30:
        return (
            "Low Suspicion",
            f"The AI model found no significant patterns matching known vulnerable "
            f"contracts (probability: {prob*100:.1f}%). This contract's code structure "
            f"does not statistically resemble exploited contracts in the training data. "
            f"Note: the ML model may still miss novel vulnerability types not seen during training."
        )

    if prob < 0.50:
        return (
            "Moderate Suspicion",
            f"The AI model detected some patterns that resemble vulnerable contracts "
            f"(probability: {prob*100:.1f}%). This is above the detection threshold of 30%. "
            f"The contract shares statistical similarities with contracts that have had "
            f"security issues. Manual review is recommended."
        )

    if prob < 0.75:
        return (
            "High Suspicion",
            f"The AI model is fairly confident this contract contains vulnerability "
            f"patterns (probability: {prob*100:.1f}%). The contract's code structure "
            f"is statistically similar to contracts that have been exploited. "
            f"Fix the identified issues before any mainnet deployment."
        )

    if prob < 0.90:
        return (
            "Very High Suspicion",
            f"The AI model is highly confident this contract is vulnerable "
            f"(probability: {prob*100:.1f}%). Strong statistical similarity to known "
            f"vulnerable contracts detected across multiple code patterns. "
            f"Do not deploy without a full security audit."
        )

    return (
        "Extremely High Suspicion",
        f"The AI model is extremely confident this contract contains critical "
        f"vulnerabilities (probability: {prob*100:.1f}%). This contract closely "
        f"matches patterns of contracts that have been exploited on mainnet. "
        f"Do NOT deploy under any circumstances without complete remediation."
    )


# ── ML Impact Explanation ──────────────────────────────────────────────────────
def explain_ml_impact(prob: float, impact: int) -> str:
    if prob < 0.30:
        return f"ML model contributed 0 points to risk score (probability {prob*100:.1f}% is below the 30% detection threshold)."
    return (
        f"ML model contributed {impact} points to the risk score. "
        f"CodeBERT assigned {prob*100:.1f}% vulnerability probability based on "
        f"statistical patterns learned from 17,309 smart contracts."
    )


# ── Request Schema ─────────────────────────────────────────────────────────────
class ContractRequest(BaseModel):
    source_code: str


# ── Predict Endpoint ───────────────────────────────────────────────────────────
@app.post("/predict")
def predict(req: ContractRequest):
    code = req.source_code

    # ── 1. Run rule-based checks ───────────────────────────────────────────────
    rule_names        = []   # short keys:  ["reentrancy", "tx_origin"]
    rule_descriptions = []   # full English: ["Reentrancy vulnerability: your contract..."]

    for rule in RULES:
        flagged, name, description = rule(code)
        if flagged:
            rule_names.append(name)
            rule_descriptions.append(description)

    # ── 2. Run ML model ────────────────────────────────────────────────────────
    inputs = tokenizer(
        code, truncation=True, padding="max_length",
        max_length=MAX_LENGTH, return_tensors="pt"
    )
    inputs = {k: v.to(DEVICE) for k, v in inputs.items()}

    with torch.no_grad():
        logits = model(**inputs).logits

    prob          = torch.softmax(logits, dim=1)[0][1].item()
    ml_prediction = 1 if prob >= THRESHOLD else 0

    # ── 3. Confidence classification ───────────────────────────────────────────
    confidence_level, confidence_explanation = classify_confidence(prob)

    # ── 4. ML impact on risk score (mirrors Java RiskScoringService) ───────────
    if   prob < 0.30: ml_impact = 0
    elif prob > 0.90: ml_impact = 20
    elif prob > 0.75: ml_impact = 12
    elif prob > 0.60: ml_impact = 6
    else:             ml_impact = int(prob * 20)

    ml_impact_explanation = explain_ml_impact(prob, ml_impact)

    # ── 5. Hybrid decision: VULNERABLE if ML OR any rule fires ────────────────
    final_prediction = 1 if (ml_prediction == 1 or len(rule_names) > 0) else 0
    verdict          = "VULNERABLE" if final_prediction == 1 else "CLEAN"

    # ── 6. Summary sentence for dashboard ─────────────────────────────────────
    if verdict == "VULNERABLE":
        if rule_names and ml_prediction == 1:
            summary = (f"Contract flagged by BOTH the AI model ({prob*100:.1f}% probability) "
                       f"AND {len(rule_names)} rule-based detector(s). "
                       f"High confidence this contract is exploitable.")
        elif rule_names:
            summary = (f"Contract flagged by {len(rule_names)} rule-based detector(s). "
                       f"The AI model did not independently flag it ({prob*100:.1f}%), "
                       f"but rule matches indicate known vulnerability patterns.")
        else:
            summary = (f"Contract flagged by AI model only ({prob*100:.1f}% probability). "
                       f"No specific rule matched, but statistical patterns resemble "
                       f"known vulnerable contracts. Manual review recommended.")
    else:
        summary = (f"No vulnerabilities detected. AI model probability: {prob*100:.1f}% "
                   f"(below 30% threshold). No rule-based patterns matched. "
                   f"Consider a professional audit before mainnet deployment.")

    return {
        # ── Core ML output ─────────────────────────────────────────────────────
        "ml_probability":          round(prob, 4),
        "ml_prediction":           ml_prediction,
        "final_prediction":        final_prediction,
        "verdict":                 verdict,

        # ── Confidence (human-readable) ────────────────────────────────────────
        "confidence_level":        confidence_level,
        "confidence_explanation":  confidence_explanation,

        # ── Rule flags (both short keys and full explanations) ─────────────────
        "rule_names":              rule_names,
        "rule_descriptions":       rule_descriptions,

        # ── ML impact on risk score ────────────────────────────────────────────
        "ml_impact":               ml_impact,
        "ml_impact_explanation":   ml_impact_explanation,

        # ── One-line dashboard summary ─────────────────────────────────────────
        "summary":                 summary
    }